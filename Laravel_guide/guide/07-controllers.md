# 07 - Controllers

Controllers organize request handling logic and keep routes clean. This chapter covers controller patterns, dependency injection, resource controllers, and best practices for Laravel API development.

## Overview

| Controller Type | Use Case |
|----------------|----------|
| Basic Controller | Simple endpoints |
| Resource Controller | CRUD operations |
| Invokable Controller | Single-action endpoints |
| API Resource Controller | RESTful APIs without create/edit forms |

## Creating Controllers

### Basic Controller

```bash
php artisan make:controller UserController
```

```php
<?php

namespace App\Http\Controllers;

use App\Models\User;
use Illuminate\Http\JsonResponse;

class UserController extends Controller
{
    public function index(): JsonResponse
    {
        $users = User::query()
            ->select(['id', 'name', 'email', 'created_at'])
            ->latest()
            ->paginate(20);

        return response()->json($users);
    }

    public function show(User $user): JsonResponse
    {
        return response()->json($user);
    }
}
```

### API Resource Controller

```bash
php artisan make:controller Api/PostController --api
```

This generates a controller with `index`, `store`, `show`, `update`, and `destroy` methods (no `create` or `edit` for APIs).

```php
<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StorePostRequest;
use App\Http\Requests\UpdatePostRequest;
use App\Http\Resources\PostResource;
use App\Models\Post;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;

class PostController extends Controller
{
    /**
     * Display a listing of posts.
     */
    public function index(): AnonymousResourceCollection
    {
        $posts = Post::query()
            ->with('author:id,name')
            ->latest()
            ->paginate(20);

        return PostResource::collection($posts);
    }

    /**
     * Store a newly created post.
     */
    public function store(StorePostRequest $request): JsonResponse
    {
        $post = Post::create([
            ...$request->validated(),
            'user_id' => $request->user()->id,
        ]);

        return response()->json(
            new PostResource($post->load('author')),
            201
        );
    }

    /**
     * Display the specified post.
     */
    public function show(Post $post): PostResource
    {
        return new PostResource($post->load(['author', 'comments']));
    }

    /**
     * Update the specified post.
     */
    public function update(UpdatePostRequest $request, Post $post): PostResource
    {
        $post->update($request->validated());

        return new PostResource($post->fresh());
    }

    /**
     * Remove the specified post.
     */
    public function destroy(Post $post): JsonResponse
    {
        $post->delete();

        return response()->json(null, 204);
    }
}
```

### Invokable Controller (Single Action)

```bash
php artisan make:controller HealthCheckController --invokable
```

```php
<?php

namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Cache;

class HealthCheckController extends Controller
{
    public function __invoke(): JsonResponse
    {
        $health = [
            'status' => 'healthy',
            'timestamp' => now()->toIso8601String(),
            'checks' => [
                'database' => $this->checkDatabase(),
                'cache' => $this->checkCache(),
            ],
        ];

        $status = collect($health['checks'])->every(fn ($check) => $check['status'] === 'ok')
            ? 200
            : 503;

        return response()->json($health, $status);
    }

    private function checkDatabase(): array
    {
        try {
            DB::connection()->getPdo();
            return ['status' => 'ok'];
        } catch (\Exception $e) {
            return ['status' => 'error', 'message' => 'Database connection failed'];
        }
    }

    private function checkCache(): array
    {
        try {
            Cache::put('health_check', true, 10);
            Cache::forget('health_check');
            return ['status' => 'ok'];
        } catch (\Exception $e) {
            return ['status' => 'error', 'message' => 'Cache not available'];
        }
    }
}
```

## Dependency Injection

### Constructor Injection

```php
<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Services\DoctorService;
use App\Http\Requests\StoreDoctorRequest;
use Illuminate\Http\JsonResponse;

class DoctorController extends Controller
{
    public function __construct(
        private readonly DoctorService $doctorService
    ) {}

    public function index(): JsonResponse
    {
        $doctors = $this->doctorService->getAllActive();
        return response()->json($doctors);
    }

    public function store(StoreDoctorRequest $request): JsonResponse
    {
        $doctor = $this->doctorService->create($request->validated());
        return response()->json($doctor, 201);
    }
}
```

### Method Injection

```php
public function store(
    StoreDoctorRequest $request,
    DoctorService $service,
    NotificationService $notifier
): JsonResponse {
    $doctor = $service->create($request->validated());
    $notifier->sendWelcomeEmail($doctor);

    return response()->json($doctor, 201);
}
```

### Contextual Binding

```php
// app/Providers/AppServiceProvider.php
use App\Http\Controllers\Api\PaymentController;
use App\Services\Payment\StripePaymentGateway;
use App\Services\Payment\PaymentGatewayInterface;

public function register(): void
{
    $this->app->when(PaymentController::class)
        ->needs(PaymentGatewayInterface::class)
        ->give(StripePaymentGateway::class);
}
```

## Route Model Binding

### Implicit Binding

```php
// routes/api.php
Route::get('/doctors/{doctor}', [DoctorController::class, 'show']);

// Controller - $doctor is automatically resolved
public function show(Doctor $doctor): JsonResponse
{
    return response()->json($doctor);
}
```

### Custom Key Binding

```php
// In the model
class Doctor extends Model
{
    public function getRouteKeyName(): string
    {
        return 'slug'; // Use slug instead of id
    }
}

// Or in route definition
Route::get('/doctors/{doctor:slug}', [DoctorController::class, 'show']);
```

### Scoped Binding

```php
// Nested resource - appointment must belong to doctor
Route::get('/doctors/{doctor}/appointments/{appointment}', function (Doctor $doctor, Appointment $appointment) {
    return $appointment;
})->scopeBindings();
```

### Custom Resolution Logic

```php
// In RouteServiceProvider
public function boot(): void
{
    Route::bind('doctor', function (string $value) {
        return Doctor::query()
            ->where('id', $value)
            ->orWhere('slug', $value)
            ->firstOrFail();
    });
}
```

### Missing Model Handling

```php
Route::get('/doctors/{doctor}', [DoctorController::class, 'show'])
    ->missing(function () {
        return response()->json([
            'message' => 'Doctor not found',
        ], 404);
    });
```

## Controller Middleware

### Apply in Constructor

```php
class DoctorController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth:sanctum');
        $this->middleware('throttle:60,1')->only(['store', 'update']);
        $this->middleware('can:delete,doctor')->only('destroy');
    }
}
```

### Apply in Routes

```php
Route::middleware(['auth:sanctum', 'verified'])->group(function () {
    Route::apiResource('doctors', DoctorController::class);
});
```

## Response Helpers

### JSON Responses

```php
class DoctorController extends Controller
{
    public function index(): JsonResponse
    {
        $doctors = Doctor::paginate(20);

        return response()->json([
            'data' => $doctors->items(),
            'meta' => [
                'current_page' => $doctors->currentPage(),
                'last_page' => $doctors->lastPage(),
                'per_page' => $doctors->perPage(),
                'total' => $doctors->total(),
            ],
        ]);
    }

    public function store(StoreDoctorRequest $request): JsonResponse
    {
        $doctor = Doctor::create($request->validated());

        return response()->json($doctor, 201)
            ->header('X-Resource-Id', $doctor->id);
    }

    public function destroy(Doctor $doctor): JsonResponse
    {
        $doctor->delete();

        return response()->json(null, 204);
    }
}
```

### Download Responses

```php
public function export(): BinaryFileResponse
{
    $doctors = Doctor::all();
    $csv = $this->generateCsv($doctors);

    return response()->download(
        storage_path('exports/doctors.csv'),
        'doctors.csv',
        ['Content-Type' => 'text/csv']
    );
}

public function stream(): StreamedResponse
{
    return response()->stream(function () {
        $handle = fopen('php://output', 'w');
        fputcsv($handle, ['ID', 'Name', 'Email']);

        Doctor::chunk(100, function ($doctors) use ($handle) {
            foreach ($doctors as $doctor) {
                fputcsv($handle, [$doctor->id, $doctor->name, $doctor->email]);
            }
        });

        fclose($handle);
    }, 200, [
        'Content-Type' => 'text/csv',
        'Content-Disposition' => 'attachment; filename="doctors.csv"',
    ]);
}
```

## Nested Resource Controllers

### Generate Nested Controller

```bash
php artisan make:controller Api/DoctorAppointmentController --api --model=Appointment --parent=Doctor
```

```php
<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Doctor;
use App\Models\Appointment;
use App\Http\Resources\AppointmentResource;
use Illuminate\Http\JsonResponse;

class DoctorAppointmentController extends Controller
{
    public function index(Doctor $doctor): AnonymousResourceCollection
    {
        $appointments = $doctor->appointments()
            ->with('patient')
            ->latest('scheduled_at')
            ->paginate(20);

        return AppointmentResource::collection($appointments);
    }

    public function store(StoreAppointmentRequest $request, Doctor $doctor): JsonResponse
    {
        $appointment = $doctor->appointments()->create([
            ...$request->validated(),
            'status' => 'pending',
        ]);

        return response()->json(
            new AppointmentResource($appointment),
            201
        );
    }

    public function show(Doctor $doctor, Appointment $appointment): AppointmentResource
    {
        // Scoped binding ensures appointment belongs to doctor
        return new AppointmentResource($appointment->load('patient'));
    }

    public function destroy(Doctor $doctor, Appointment $appointment): JsonResponse
    {
        $appointment->delete();
        return response()->json(null, 204);
    }
}
```

### Register Nested Routes

```php
// routes/api.php
Route::apiResource('doctors.appointments', DoctorAppointmentController::class)
    ->scoped()
    ->shallow(); // Creates /appointments/{appointment} for show/update/destroy
```

## Custom Actions

### Adding Custom Actions

```php
class DoctorController extends Controller
{
    // Standard CRUD methods...

    /**
     * Get doctor's schedule for a specific date range.
     */
    public function schedule(Doctor $doctor, Request $request): JsonResponse
    {
        $request->validate([
            'start_date' => 'required|date',
            'end_date' => 'required|date|after_or_equal:start_date',
        ]);

        $schedule = $doctor->appointments()
            ->whereBetween('scheduled_at', [
                $request->start_date,
                $request->end_date,
            ])
            ->with('patient:id,name')
            ->get();

        return response()->json(['schedule' => $schedule]);
    }

    /**
     * Activate a doctor.
     */
    public function activate(Doctor $doctor): JsonResponse
    {
        $doctor->update(['is_active' => true]);

        return response()->json([
            'message' => 'Doctor activated successfully',
            'doctor' => $doctor->fresh(),
        ]);
    }

    /**
     * Deactivate a doctor.
     */
    public function deactivate(Doctor $doctor): JsonResponse
    {
        $doctor->update(['is_active' => false]);

        // Cancel pending appointments
        $doctor->appointments()
            ->where('status', 'pending')
            ->update(['status' => 'cancelled']);

        return response()->json([
            'message' => 'Doctor deactivated successfully',
        ]);
    }
}
```

### Register Custom Routes

```php
// routes/api.php
Route::prefix('doctors/{doctor}')->group(function () {
    Route::get('schedule', [DoctorController::class, 'schedule']);
    Route::post('activate', [DoctorController::class, 'activate']);
    Route::post('deactivate', [DoctorController::class, 'deactivate']);
});
```

## Authorization in Controllers

### Using Policies

```php
class DoctorController extends Controller
{
    public function __construct()
    {
        $this->authorizeResource(Doctor::class, 'doctor');
    }

    // Authorization is automatic for CRUD methods
}
```

### Manual Authorization

```php
public function update(UpdateDoctorRequest $request, Doctor $doctor): JsonResponse
{
    $this->authorize('update', $doctor);

    $doctor->update($request->validated());

    return response()->json($doctor);
}

public function destroy(Doctor $doctor): JsonResponse
{
    if ($request->user()->cannot('delete', $doctor)) {
        abort(403, 'You cannot delete this doctor');
    }

    $doctor->delete();

    return response()->json(null, 204);
}
```

## Error Handling in Controllers

### Consistent Error Responses

```php
<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\JsonResponse;

abstract class ApiController extends Controller
{
    protected function success(mixed $data, int $status = 200): JsonResponse
    {
        return response()->json([
            'success' => true,
            'data' => $data,
        ], $status);
    }

    protected function created(mixed $data): JsonResponse
    {
        return $this->success($data, 201);
    }

    protected function noContent(): JsonResponse
    {
        return response()->json(null, 204);
    }

    protected function error(string $message, int $status = 400, array $errors = []): JsonResponse
    {
        $response = [
            'success' => false,
            'message' => $message,
        ];

        if (!empty($errors)) {
            $response['errors'] = $errors;
        }

        return response()->json($response, $status);
    }

    protected function notFound(string $message = 'Resource not found'): JsonResponse
    {
        return $this->error($message, 404);
    }

    protected function forbidden(string $message = 'Access denied'): JsonResponse
    {
        return $this->error($message, 403);
    }
}
```

```php
class DoctorController extends ApiController
{
    public function show(Doctor $doctor): JsonResponse
    {
        return $this->success(new DoctorResource($doctor));
    }

    public function store(StoreDoctorRequest $request): JsonResponse
    {
        $doctor = Doctor::create($request->validated());
        return $this->created(new DoctorResource($doctor));
    }

    public function destroy(Doctor $doctor): JsonResponse
    {
        if ($doctor->appointments()->pending()->exists()) {
            return $this->error('Cannot delete doctor with pending appointments', 422);
        }

        $doctor->delete();
        return $this->noContent();
    }
}
```

## Testing Controllers

### Feature Tests

```php
<?php

namespace Tests\Feature;

use App\Models\Doctor;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class DoctorControllerTest extends TestCase
{
    use RefreshDatabase;

    public function test_can_list_doctors(): void
    {
        Doctor::factory()->count(5)->create();

        $response = $this->getJson('/api/doctors');

        $response->assertOk()
            ->assertJsonCount(5, 'data')
            ->assertJsonStructure([
                'data' => [
                    '*' => ['id', 'name', 'specialty', 'created_at'],
                ],
            ]);
    }

    public function test_can_create_doctor(): void
    {
        $user = User::factory()->admin()->create();

        $response = $this->actingAs($user)
            ->postJson('/api/doctors', [
                'name' => 'Dr. John Smith',
                'specialty' => 'Cardiology',
                'email' => 'john@hospital.com',
            ]);

        $response->assertCreated()
            ->assertJsonPath('data.name', 'Dr. John Smith');

        $this->assertDatabaseHas('doctors', [
            'email' => 'john@hospital.com',
        ]);
    }

    public function test_cannot_create_doctor_without_auth(): void
    {
        $response = $this->postJson('/api/doctors', [
            'name' => 'Dr. John Smith',
        ]);

        $response->assertUnauthorized();
    }

    public function test_can_delete_doctor(): void
    {
        $user = User::factory()->admin()->create();
        $doctor = Doctor::factory()->create();

        $response = $this->actingAs($user)
            ->deleteJson("/api/doctors/{$doctor->id}");

        $response->assertNoContent();
        $this->assertDatabaseMissing('doctors', ['id' => $doctor->id]);
    }
}
```

## Best Practices

1. **Keep controllers thin** - Move business logic to services or actions
2. **Use Form Requests** - Separate validation from controllers
3. **Use API Resources** - Transform data consistently
4. **Single Responsibility** - One controller per resource
5. **Type hints** - Use PHP 8 features for clarity
6. **Dependency injection** - Inject services, not facades
7. **Consistent responses** - Use a base API controller
8. **Authorization** - Use policies for access control
9. **Documentation** - Add PHPDoc comments
10. **Test thoroughly** - Feature tests for all endpoints

## References

- [Laravel Controllers](https://laravel.com/docs/11.x/controllers)
- [Route Model Binding](https://laravel.com/docs/11.x/routing#route-model-binding)
- [Middleware](https://laravel.com/docs/11.x/middleware)

---

[Previous: Routing](./06-routing.md) | [Back to Index](./README.md) | [Next: Eloquent ORM and Models](./08-eloquent-orm-models.md)
