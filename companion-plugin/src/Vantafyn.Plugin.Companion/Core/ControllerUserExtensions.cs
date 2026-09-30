using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Mvc;

namespace Vantafyn.Plugin.Companion.Core;

internal static class ControllerUserExtensions
{
    public static async Task<Guid> CurrentUserIdAsync(this ControllerBase controller, IAuthorizationContext authorizationContext)
    {
        var auth = await authorizationContext.GetAuthorizationInfo(controller.Request).ConfigureAwait(false);
        if (auth.UserId == Guid.Empty)
        {
            throw new UnauthorizedAccessException("Authenticated Jellyfin user could not be resolved.");
        }

        return auth.UserId;
    }

    public static Guid CurrentUserId(this ControllerBase controller, IAuthorizationContext authorizationContext) =>
        controller.CurrentUserIdAsync(authorizationContext).GetAwaiter().GetResult();

    public static async Task<string> CurrentUserNameAsync(this ControllerBase controller, IAuthorizationContext authorizationContext)
    {
        try
        {
            var auth = await authorizationContext.GetAuthorizationInfo(controller.Request).ConfigureAwait(false);
            return auth.User?.Username ?? "Trainer";
        }
        catch
        {
            return "Trainer";
        }
    }
}
