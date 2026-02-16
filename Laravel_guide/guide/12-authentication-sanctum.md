# 12 - API Authentication with Laravel Sanctum

Laravel Sanctum provides a lightweight authentication system for APIs and SPAs. Use it when you want simple token auth without full OAuth complexity.

## Goals

- Install Sanctum the Laravel 12 way
- Issue, scope, and revoke API tokens
- Support SPA authentication safely

## 1. When to Use Sanctum

- **API tokens** for mobile apps, CLI tools, or third‑party integrations.
- **SPA authentication** when your frontend and backend share a domain or trusted subdomains.

If you need OAuth2 flows for third‑party clients, consider Laravel Passport instead.

## 2. Install Sanctum (Laravel 12)

For a fresh API setup, use the built‑in installer:

```bash
php artisan install:api
```

This command installs Sanctum, publishes config, and adds the API routes scaffold.

For existing projects:

```bash
composer require laravel/sanctum
php artisan vendor:publish --provider="Laravel\\Sanctum\\SanctumServiceProvider"
php artisan migrate
```

## 3. Add the Trait to the User Model

```php
// app/Models/User.php
namespace App\Models;

use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable
{
    use HasApiTokens, Notifiable;
}
```

## 4. Issue Tokens on Login

```php
// app/Http/Controllers/Api/AuthController.php
namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class AuthController extends Controller
{
    public function login(Request $request)
    {
        $credentials = $request->validate([
            'email' => ['required', 'email'],
            'password' => ['required'],
        ]);

        if (! Auth::attempt($credentials)) {
            return response()->json(['message' => 'Invalid credentials'], 401);
        }

        $user = $request->user();
        $token = $user->createToken('api-token', ['posts:read', 'posts:write']);

        return response()->json([
            'token' => $token->plainTextToken,
            'user' => $user,
        ]);
    }
}
```

## 5. Protect Routes with `auth:sanctum`

```php
use App\Http\Controllers\PostController;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

Route::middleware('auth:sanctum')->group(function () {
    Route::get('/me', fn (Request $request) => $request->user());
    Route::apiResource('posts', PostController::class);
});
```

Clients send:

```
Authorization: Bearer <token>
```

## 6. Token Abilities (Scopes)

```php
if ($request->user()->tokenCan('posts:write')) {
    // Allowed
}
```

Use abilities to issue least‑privilege tokens for integrations.

## 7. Revoking Tokens

```php
// revoke current token
$request->user()->currentAccessToken()->delete();

// revoke all tokens
$request->user()->tokens()->delete();
```

## 8. Token Expiration

Set expiration in `config/sanctum.php`:

```php
'expiration' => 60 * 24, // minutes
```

Expired tokens are treated as invalid automatically.

## 9. SPA Authentication (Stateful)

For SPAs, Sanctum uses session authentication with CSRF protection.

- Add your SPA domain to `SANCTUM_STATEFUL_DOMAINS`.
- Include `EnsureFrontendRequestsAreStateful` in the `api` middleware group.
- Fetch the CSRF cookie before login:

```http
GET /sanctum/csrf-cookie
```

## Tips

- Store tokens securely and rotate them when possible.
- Rate‑limit login and token endpoints.
- Use HTTPS for all production traffic.

---

[Previous: Middleware](./11-middleware.md) | [Back to Index](./README.md) | [Next: API Resources ->](./13-api-resources.md)
