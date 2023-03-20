using JetBrains.Annotations;
using JetBrains.Application.Notifications;
using JetBrains.Application.Threading;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.Util;

namespace UtBot.Utils;

[SolutionComponent]
internal class Notifications
{
    private Lifetime _lifetime;
    private readonly SequentialLifetimes _sequentialLifetimes;
    private readonly UserNotifications _userNotifications;
    private readonly IShellLocks _shellLocks;
    private readonly ILogger _logger;

    private const string Title = "UnitTestBot.NET";

    public Notifications(
        Lifetime lifetime,
        UserNotifications userNotifications,
        IShellLocks shellLocks,
        ILogger logger)
    {
        _sequentialLifetimes = new SequentialLifetimes(lifetime);
        _lifetime = _sequentialLifetimes.Next();
        _userNotifications = userNotifications;
        _shellLocks = shellLocks;
        _logger = logger;
    }

    private void ShowNotification(
        NotificationSeverity severity,
        string title,
        string body,
        bool closeAfterExecution = true,
        UserNotificationCommand command = null)
    {
        void NotificationAction()
        {
            _userNotifications.CreateNotification(
                _lifetime,
                severity,
                title,
                body,
                closeAfterExecution: closeAfterExecution,
                executed: command);
        }

        _shellLocks.ExecuteOrQueueEx(
            _lifetime,
            "UtBot::Notification::Show",
            NotificationAction);
    }

    public void ShowError(
        [NotNull] string body,
        bool closeAfterExecution = true,
        UserNotificationCommand command = null)
    {
        ShowNotification(
            NotificationSeverity.CRITICAL,
            Title,
            body,
            closeAfterExecution: closeAfterExecution,
            command: command);
    }

    public void ShowInfo(
        [NotNull] string body,
        bool closeAfterExecution = true,
        UserNotificationCommand command = null)
    {
        ShowNotification(
            NotificationSeverity.INFO,
            Title,
            body,
            closeAfterExecution: closeAfterExecution,
            command: command);
    }

    public void ShowWarning(
        [NotNull] string body,
        bool closeAfterExecution = true,
        UserNotificationCommand command = null)
    {
        ShowNotification(
            NotificationSeverity.WARNING,
            Title,
            body,
            closeAfterExecution: closeAfterExecution,
            command: command);
    }

    public void Refresh() => _lifetime = _sequentialLifetimes.Next();
}
