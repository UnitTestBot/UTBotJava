using System;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Net;
using JetBrains.Annotations;
using JetBrains.Application.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using JetBrains.Threading;
using JetBrains.Util;
using JetBrains.Util.Logging;
using UtBot.Rd;
using UtBot.Rd.Generated;

namespace UtBot;

[SuppressMessage("ReSharper", "MemberCanBePrivate.Global")]
public class ProcessWithRdServer
{
    public Lifetime Lifetime => _ldef.Lifetime;
    public Protocol Protocol;
    [CanBeNull] public VSharpModel VSharpModel { get; private set; }

    private readonly LifetimeDefinition _ldef;
    [CanBeNull] private Process _process;

    public ProcessWithRdServer(string name, string workingDir, int port, string exePath, IShellLocks shellLocks, Lifetime? parent = null, [CanBeNull] ILogger logger = null)
    {
        logger ??= Logger.GetLogger<ProcessWithRdServer>();
        using var blockingCollection = new BlockingCollection<String>(2);
        shellLocks.AssertNonMainThread();
        _ldef = (parent ?? Lifetime.Eternal).CreateNested();
        var pingLdef = _ldef.Lifetime.CreateNested();
        try
        {
            SingleThreadScheduler.RunOnSeparateThread(Lifetime, name, scheduler =>
            {
                var endPoint = new IPEndPoint(IPAddress.Loopback, port);
                var socket = SocketWire.Server.CreateServerSocket(endPoint);
                var wire = new SocketWire.Server(Lifetime, scheduler, socket);
                var serializers = new Serializers();
                var identities = new Identities(IdKind.Server);
                var startInfo = new ProcessStartInfo("dotnet", $"--roll-forward LatestMajor \"{exePath}\" {port}")
                {
                    WorkingDirectory = workingDir
                };

                Protocol = new Protocol(name, serializers, identities, scheduler, wire, Lifetime);
                scheduler.Queue(() =>
                {
                    VSharpModel = new VSharpModel(Lifetime, Protocol);
                    VSharpModel.Ping.Advise(pingLdef.Lifetime, s =>
                    {
                        if (s == name)
                        {
                            blockingCollection.TryAdd(s);
                        }
                    });
                    VSharpModel.Log.Advise(Lifetime, s => logger.Info($"V#: {s}"));
                });
                _process = new Process();
                _process.StartInfo = startInfo;
                Lifetime.OnTermination(() => _process.Kill(entireProcessTree: true));
                if (_process.Start())
                    _process.Exited += (_, _) => _ldef.Terminate();
                else
                    _ldef.Terminate();
            });

            if (_process?.HasExited == true) return;

            SpinWaitEx.SpinUntil(pingLdef.Lifetime, () =>
            {
                if (_process?.HasExited == true)
                {
                    VSharpModel = null;
                    _ldef.Terminate();
                }

                VSharpModel?.Ping.Fire(RdUtil.MainProcessName);
                return blockingCollection.TryTake(out _);
            });
            pingLdef.Terminate();
        }
        catch (Exception)
        {
            _ldef.Terminate();
            throw;
        }
    }
}
