using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Mvc;

namespace Vantafyn.Plugin.Companion.Core;

internal static class ControllerUserExtensions
{
    public static Guid CurrentUserId(this ControllerBase controller, IAuthorizationContext authorizationContext)
    {
        var auth = authorizationContext.GetAuthorizationInfo(controller.Request).GetAwaiter().GetResult();
        if (auth.UserId == Guid.Empty)
        {
            throw new UnauthorizedAccessException("Authenticated Jellyfin user could not be resolved.");
        }

        return auth.UserId;
    }
}
