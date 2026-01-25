# 12 - API Authentication with Laravel Sanctum

Securing your API is a non-negotiable step in backend development. Laravel Sanctum provides a featherweight, simple-to-use system for authenticating Single Page Applications (SPAs), mobile applications, and other services via API tokens. This guide will focus on the **API token authentication** mechanism, which is perfect for most backend API use cases.

---

## 1. What is Laravel Sanctum?

Sanctum solves two main problems:

1.  **API Tokens**: It allows you to issue API tokens to your users, who can then use these tokens to authenticate their requests. These tokens are typically long-lived and can be revoked manually. This is ideal for mobile apps or third-party services.
2.  **SPA Authentication**: It provides a simple way to authenticate SPAs (like React, Vue, or Angular apps) that live on a separate domain from your API, using Laravel's session-based authentication in a stateful way.

We will focus on **API Tokens**.

---

## 2. Installation and Configuration

### Step 1: Install Sanctum
```bash
composer require laravel/sanctum
```

### Step 2: Publish and Migrate
Publish the Sanctum configuration and migration files. Then, run the migration to create the `personal_access_tokens` table in your database.

```bash
php artisan vendor:publish --provider="Laravel\Sanctum\SanctumServiceProvider"
php artisan migrate
```

### Step 3: Configure API Middleware
For API token authentication, ensure that the `EnsureFrontendRequestsAreStateful` middleware is *not* present in your `api` middleware group in `app/Http/Kernel.php`. This middleware is for SPA authentication. Your `api` group should look something like this:

```php
// app/Http/Kernel.php
'api' => [
    \Illuminate\Routing\Middleware\ThrottleRequests::class.':api',
    \Illuminate\Routing\Middleware\SubstituteBindings::class,
],
```

### Step 4: Add Trait to User Model
Add the `HasApiTokens` trait to your `User` model. This trait provides the necessary methods to issue and manage tokens.

```php
// app/Models/User.php
use Laravel\Sanctum\HasApiTokens;
use Illuminate\Notifications\Notifiable;
use Illuminate\Foundation\Auth\User as Authenticatable;

class User extends Authenticatable
{
    use HasApiTokens, Notifiable;
    // ...
}
```

---

## 3. Issuing API Tokens

The most common scenario for issuing tokens is during user login.

### Create a Login Route and Controller
First, create a controller to handle authentication:
```bash
php artisan make:controller Api/AuthController
```

Next, define the login route in `routes/api.php`:
```php
// routes/api.php
use App\Http\Controllers\Api\AuthController;

Route::post('/login', [AuthController::class, 'login']);
```

### Implement the Login Logic
In your `AuthController`, validate the user's credentials and issue a token upon successful login.

```php
// app/Http/Controllers/Api/AuthController.php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Http\JsonResponse;

class AuthController extends Controller
{
    public function login(Request $request): JsonResponse
    {
        $credentials = $request->validate([
            'email' => 'required|email',
            'password' => 'required',
        ]);

        if (!Auth::attempt($credentials)) {
            return response()->json(['message' => 'Invalid credentials'], 401);
        }

        $user = $request->user();
        $token = $user->createToken('auth-token')->plainTextToken;

        return response()->json([
            'message' => 'Login successful',
            'token' => $token,
            'user' => $user,
        ]);
    }
}
```

---

## 4. Protecting Routes

To protect your API routes, you can use the `auth:sanctum` middleware. Any request to these routes must include a valid `Authorization` header.

```php
// routes/api.php
Route::middleware('auth:sanctum')->group(function () {
    Route::get('/user/profile', function (Request $request) {
        return $request->user();
    });

    Route::apiResource('doctors', DoctorController::class);
    // ... other protected routes
});
```

---

## 5. Authenticating Requests

When a client makes a request to a protected route, it must include an `Authorization` header with the API token.

**Header:** `Authorization: Bearer <your-plain-text-token>`

Inside your controller, you can access the authenticated user via the `Request` object.

```php
// In a controller method protected by auth:sanctum
public function show(Request $request)
{
    $user = $request->user(); // Returns the authenticated user instance
    // ...
}
```

---

## 6. Token Abilities (Scopes)

Sanctum allows you to assign "abilities" or "scopes" to tokens, which act as a simple permission system.

### Creating Tokens with Abilities
```php
// Create a token that can only perform server-related tasks
$token = $user->createToken('server-token', ['server:update', 'server:read']);

// Create a token with fewer permissions
$token = $user->createToken('read-only-token', ['server:read']);
```

### Checking for Abilities
You can check if a token has a specific ability using the `tokenCan()` method.

```php
// In a middleware or controller
if ($request->user()->tokenCan('server:update')) {
    // Allow action
} else {
    // Deny action
}
```

---

## 7. Revoking Tokens

You can revoke tokens to invalidate them. A common place to do this is in a logout endpoint.

```php
// routes/api.php
Route::middleware('auth:sanctum')->post('/logout', [AuthController::class, 'logout']);
```

```php
// app/Http/Controllers/Api/AuthController.php
public function logout(Request $request): JsonResponse
{
    // Revoke the token that was used to authenticate the current request...
    $request->user()->currentAccessToken()->delete();
    
    // To revoke all tokens for the user:
    // $request->user()->tokens()->delete();

    return response()->json(['message' => 'Logged out successfully']);
}
```

By integrating Sanctum, you have a secure and straightforward way to manage API authentication, which is essential for any modern backend application.