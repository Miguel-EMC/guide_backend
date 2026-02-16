# 10 - Requests and Validation

This chapter covers request handling and validation, including Form Requests, custom rules, and advanced patterns for API backends.

## Goals

- Validate at the boundary
- Keep validation logic reusable
- Return consistent error responses

## 1. Accessing Request Data

```php
use Illuminate\Http\Request;

public function store(Request $request)
{
    $name = $request->input('name');
    $email = $request->string('email');
    $active = $request->boolean('is_active');
}
```

## 2. Quick Validation in Controllers

```php
use App\Models\User;
use Illuminate\Http\Request;

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

## 3. Form Requests (Recommended)

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

    public function attributes(): array
    {
        return [
            'email' => 'email address',
        ];
    }
}
```

Use it in a controller:

```php
use App\Http\Requests\StoreUserRequest;
use App\Models\User;

public function store(StoreUserRequest $request)
{
    $data = $request->validated();
    return User::create($data);
}
```

## 4. Preparing Input Before Validation

```php
protected function prepareForValidation(): void
{
    $this->merge([
        'email' => strtolower($this->input('email')),
    ]);
}
```

## 5. After Validation Hook

```php
public function withValidator($validator): void
{
    $validator->after(function ($validator) {
        if ($this->input('type') === 'pro' && ! $this->filled('company')) {
            $validator->errors()->add('company', 'Company is required for pro accounts.');
        }
    });
}
```

## 6. Nested and Array Validation

```php
$request->validate([
    'items' => ['required', 'array', 'min:1'],
    'items.*.sku' => ['required', 'string'],
    'items.*.qty' => ['required', 'integer', 'min:1'],
]);
```

## 7. Custom Rule Classes

```bash
php artisan make:rule StrongPassword
```

```php
use Illuminate\Contracts\Validation\Rule;

class StrongPassword implements Rule
{
    public function passes($attribute, $value): bool
    {
        return strlen($value) >= 12 && preg_match('/[A-Z]/', $value);
    }

    public function message(): string
    {
        return 'The :attribute must be at least 12 characters and include uppercase.';
    }
}
```

## 8. Conditional Validation

```php
use Illuminate\Validation\Rule;

$request->validate([
    'type' => ['required', Rule::in(['basic', 'pro'])],
    'company' => [Rule::requiredIf($request->input('type') === 'pro')],
]);
```

## 9. File Validation

```php
$request->validate([
    'avatar' => ['required', 'image', 'max:2048'],
]);

$path = $request->file('avatar')->store('avatars', 'public');
```

## 10. Manual Validator

```php
use Illuminate\Support\Facades\Validator;

$validator = Validator::make($request->all(), [
    'email' => ['required', 'email'],
]);

if ($validator->fails()) {
    return response()->json(['errors' => $validator->errors()], 422);
}
```

## 11. Safe Data Extraction

Form Requests support safe data access:

```php
$data = $request->safe()->only(['name', 'email']);
```

## Tips

- Validate at the boundary, not inside services.
- Use Form Requests for complex validation rules.
- Keep error responses consistent and documented.

---

[Previous: Migrations and Seeding](./09-database-migrations-and-seeding.md) | [Back to Index](./README.md) | [Next: Middleware ->](./11-middleware.md)
