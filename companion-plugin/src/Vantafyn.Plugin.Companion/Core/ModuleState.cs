namespace Vantafyn.Plugin.Companion.Core;

public enum ModuleState
{
    Disabled,
    Unconfigured,
    Ready,
    Degraded,
    Unavailable
}

public sealed record ModuleCapability(ModuleState State, string? Message = null);

public interface IClock
{
    DateTimeOffset UtcNow { get; }
}

public sealed class SystemClock : IClock
{
    public DateTimeOffset UtcNow => DateTimeOffset.UtcNow;
}
