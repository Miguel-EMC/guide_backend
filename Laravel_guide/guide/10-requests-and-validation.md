# 10 - Requests and Validation

This chapter covers request handling and validation, including Form Requests and custom messages.

## Basic Request Access

Inject `Illuminate\Http\Request` into a controller:

```php
use Illuminate\Http\Request;

public function store(Request $request)
{
    $name = $request->input('name');
    $email = $request->input('email');
}
```

## Validating in the Controller

```php
public function store(Request $request)
{
    $data = $request->validate([
        'name' => ['required', 'string', 'max:120'],
        'email' => ['required', 'email', 'unique:users,email'],
        'password' => ['required', 'string', 'min:8'],
    ]);

    return User::create($data);
}
```

## Form Requests (Recommended)

Create a Form Request:

```bash
php artisan make:request StoreUserRequest
```

```php
namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class StoreUserRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'name' => ['required', 'string', 'max:120'],
            'email' => ['required', 'email', 'unique:users,email'],
            'password' => ['required', 'string', 'min:8'],
        ];
    }

    public function messages(): array
    {
        return [
            'email.unique' => 'Email already exists.',
        ];
    }
}
```

Use in controller:

```php
public function store(StoreUserRequest $request)
{
    $data = $request->validated();
    return User::create($data);
}
```

## Custom Attributes

```php
public function attributes(): array
{
    return [
        'email' => 'email address',
    ];
}
```

## Conditional Validation

```php
use Illuminate\Validation\Rule;

$request->validate([
    'type' => ['required', Rule::in(['basic', 'pro'])],
    'company' => [Rule::requiredIf($request->input('type') === 'pro')],
]);
```

## File Validation

```php
$request->validate([
    'avatar' => ['required', 'image', 'max:2048'],
]);

$path = $request->file('avatar')->store('avatars', 'public');
```

## API Error Response Shape

Laravel returns a `422 Unprocessable Entity` response with validation errors by default. Keep it consistent for clients.

## Tips

- Prefer Form Requests for complex validation.
- Always validate IDs and foreign keys.
- Use `validated()` to avoid mass assignment of untrusted fields.

---

[Previous: Migrations and Seeding](./09-database-migrations-and-seeding.md) | [Back to Index](./README.md) | [Next: Middleware ->](./11-middleware.md)
