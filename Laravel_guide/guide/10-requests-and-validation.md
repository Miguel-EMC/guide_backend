# 10 - Requests and Validation

In any backend application, validating incoming data is paramount for security and data integrity. Laravel provides a powerful and convenient way to validate HTTP requests. This guide focuses on handling incoming requests and ensuring their data meets your application's requirements, specifically in the context of API development.

---

## 1. Accessing Request Data

All incoming HTTP requests are encapsulated in an `Illuminate\Http\Request` object. You can inject this object into your controller methods.

```php
use Illuminate\Http\Request;

class PostController extends Controller
{
    public function store(Request $request)
    {
        // Get all input data as an array
        $allInput = $request->all();

        // Get a specific input value
        $title = $request->input('title');

        // Get a query string parameter
        $page = $request->query('page', 1); // default to 1

        // Get JSON payload data (for API POST/PUT requests)
        $data = $request->json()->all();

        // Retrieve only specific fields
        $credentials = $request->only('email', 'password');

        // Retrieve all fields except specific ones
        $safeInput = $request->except('password_confirmation');

        // Check if an input value is present
        if ($request->has('title')) {
            // ...
        }
    }
}
```

---

## 2. Basic Validation in Controllers

Laravel offers a fluent `validate()` method directly on the `Request` object. If validation fails, Laravel automatically handles the response, returning a JSON response with error messages for API requests.

```php
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;

class PostController extends Controller
{
    public function store(Request $request): JsonResponse
    {
        $validatedData = $request->validate([
            'title' => 'required|string|max:255',
            'content' => 'required|string',
            'category_id' => 'required|integer|exists:categories,id', // Ensure category exists
            'published_at' => 'nullable|date',
            'tags' => 'sometimes|array', // Optional array
            'tags.*' => 'string|max:25', // Each item in tags array must be a string
        ]);

        // Validation passed, proceed with creating the post
        // Post::create($validatedData);

        return response()->json(['message' => 'Post created successfully', 'data' => $validatedData], 201);
    }
}
```

### Common Validation Rules
Laravel provides a wide array of validation rules. Some frequently used ones include:
-   `required`: The field must be present and not empty.
-   `string`, `integer`, `boolean`, `array`: Ensure the field is of a specific type.
-   `min:value`, `max:value`: Set minimum or maximum lengths/values.
-   `email`: Validates an email address.
-   `unique:table,column`: Ensures the value is unique in a given database table and column.
-   `exists:table,column`: Ensures the value exists in a given database table and column.
-   `date`: Validates a date.
-   `sometimes`: Validates a field only if it is present in the input.

### Customizing Error Messages
You can customize the error messages by passing a second argument to the `validate` method:

```php
$validatedData = $request->validate([
    'title' => 'required|max:255',
], [
    'title.required' => 'A title is absolutely necessary!',
    'title.max' => 'The title cannot exceed 255 characters.',
]);
```

---

## 3. Form Request Validation (Recommended for APIs)

For larger applications and complex validation scenarios, **Form Request classes** offer a cleaner way to centralize validation logic. They keep your controllers thin and focused on business logic.

### Step 1: Create a Form Request
```bash
php artisan make:request StorePostRequest
```
This will create a new class at `app/Http/Requests/StorePostRequest.php`.

### Step 2: Define Rules and Authorization
Open `app/Http/Requests/StorePostRequest.php`:

```php
// app/Http/Requests/StorePostRequest.php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class StorePostRequest extends FormRequest
{
    /**
     * Determine if the user is authorized to make this request.
     */
    public function authorize(): bool
    {
        // For APIs, you might check if the user has permission to create a post.
        // For now, return true to allow all authorized users.
        return true; 
        // Example: return $this->user()->can('create', Post::class);
    }

    /**
     * Get the validation rules that apply to the request.
     *
     * @return array<string, \Illuminate\Contracts\Validation\ValidationRule|array|string>
     */
    public function rules(): array
    {
        return [
            'title' => 'required|string|max:255',
            'content' => 'required|string',
            'category_id' => 'required|integer|exists:categories,id',
            'published_at' => 'nullable|date',
            'tags' => 'sometimes|array',
            'tags.*' => 'string|max:25',
        ];
    }

    /**
     * Get the error messages for the defined validation rules.
     */
    public function messages(): array
    {
        return [
            'title.required' => 'The post title is mandatory.',
            'category_id.exists' => 'The selected category does not exist.',
        ];
    }
}
```

### Step 3: Use the Form Request in Your Controller
Now, simply type-hint the `StorePostRequest` in your controller method. Laravel will automatically run the validation, and if it passes, the method will be executed. If validation fails, an appropriate JSON response will be returned.

```php
// app/Http/Controllers/PostController.php

use App\Http\Requests\StorePostRequest; // Import your Form Request
use Illuminate\Http\JsonResponse;

class PostController extends Controller
{
    public function store(StorePostRequest $request): JsonResponse
    {
        // Validation has already passed at this point
        $validatedData = $request->validated(); // Get only the validated data

        // Post::create($validatedData);

        return response()->json(['message' => 'Post created successfully', 'data' => $validatedData], 201);
    }
}
```

---

## 4. Working with File Uploads

Laravel makes handling file uploads straightforward.

```php
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;

class ProfileController extends Controller
{
    public function updateAvatar(Request $request): JsonResponse
    {
        $request->validate([
            'avatar' => 'required|image|max:2048', // max 2MB image
        ]);

        if ($request->hasFile('avatar')) {
            // Store the file in the 'avatars' disk (defined in config/filesystems.php)
            // and get its path. Laravel automatically generates a unique filename.
            $path = $request->file('avatar')->store('avatars', 'public');

            // You can save this path in your database
            // auth()->user()->update(['avatar_path' => $path]);

            return response()->json(['message' => 'Avatar updated successfully', 'path' => Storage::url($path)]);
        }

        return response()->json(['message' => 'No avatar provided'], 400);
    }
}
```

---

## 5. API Error Responses for Validation

When validation fails for an API request, Laravel automatically returns an HTTP 422 (Unprocessable Entity) status code with a JSON response containing the validation error messages.

```json
{
    "message": "The given data was invalid.",
    "errors": {
        "title": [
            "The title field is mandatory."
        ],
        "category_id": [
            "The selected category does not exist."
        ]
    }
}
```
This consistent error handling simplifies frontend development by providing clear feedback on invalid input.