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
using JetBrains.Rider.Model;
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
    public readonly Process Proc = new();

    public ProcessWithRdServer(string name, string workingDir, int port, string exePath, IShellLocks shellLocks, UtBotRiderModel riderModel, Lifetime? parent = null, [CanBeNull] ILogger logger = null)
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
                riderModel.StartVSharp.Fire();
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
                    VSharpModel.Log.Advise(Lifetime, s =>
                    {
                        logger.Info($"V#: {s}");
                        riderModel.LogVSharp.Fire(s);
                    });
                });
                Proc.StartInfo = startInfo;
                Lifetime.OnTermination(() => Proc.Kill(entireProcessTree: true));
                if (Proc.Start())
                    Proc.Exited += (_, _) => _ldef.Terminate();
                else
                    _ldef.Terminate();
            });

            if (Proc?.HasExited == true) return;

            SpinWaitEx.SpinUntil(pingLdef.Lifetime, () =>
            {
                if (Proc?.HasExited == true)
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
