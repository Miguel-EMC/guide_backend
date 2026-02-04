# Laravel 12 Advanced Features & AI Integration

This chapter covers the advanced features introduced in Laravel 12 that are essential for modern 2026 applications, including AI integration, performance optimizations, and enterprise-ready features.

## Laravel AI SDK

Laravel 12 introduced a native AI SDK that simplifies integration with various AI providers.

### Setting Up AI Integration

```php
// config/ai.php
return [
    'default' => env('AI_DRIVER', 'openai'),
    
    'providers' => [
        'openai' => [
            'key' => env('OPENAI_API_KEY'),
            'model' => 'gpt-4o-mini',
            'max_tokens' => 2000,
        ],
        'anthropic' => [
            'key' => env('ANTHROPIC_API_KEY'),
            'model' => 'claude-3-haiku-20240307',
        ],
        'local' => [
            'endpoint' => env('LOCAL_AI_ENDPOINT'),
            'model' => 'llama-2-7b-chat',
        ],
    ],
];
```

### Basic AI Usage

```php
use Laravel\AI\Facades\AI;

// Simple chat completion
$response = AI::chat()
    ->prompt('Generate a professional email for customer support')
    ->send();

echo $response->content;

// With context and data
$user = User::find(1);
$summary = AI::chat()
    ->prompt('Summarize this user activity for the past month')
    ->withData([
        'user' => $user->toArray(),
        'activities' => $user->activities()->lastMonth()->get()->toArray()
    ])
    ->temperature(0.3)  // More deterministic
    ->maxTokens(500)
    ->send();
```

### AI-Powered Content Generation

```php
// In a controller
class PostController extends Controller
{
    public function generatePost(Request $request)
    {
        $validated = $request->validate([
            'topic' => 'required|string|max:100',
            'tone' => 'in:professional,casual,technical',
            'length' => 'in:short,medium,long',
        ]);

        $prompt = $this->buildPrompt($validated);
        
        $response = AI::chat()
            ->prompt($prompt)
            ->temperature(0.7)
            ->maxTokens($this->getMaxTokens($validated['length']))
            ->send();

        return response()->json([
            'content' => $response->content,
            'tokens_used' => $response->usage->totalTokens,
        ]);
    }

    private function buildPrompt(array $data): string
    {
        return "Write a {$data['tone']} blog post about {$data['topic']} " .
               "that is {$data['length']} and engaging for developers.";
    }

    private function getMaxTokens(string $length): int
    {
        return match($length) {
            'short' => 300,
            'medium' => 600,
            'long' => 1200,
        };
    }
}
```

### AI for Data Processing

```php
// Model with AI-powered analysis
class CustomerAnalytics extends Model
{
    public function generateInsights(): array
    {
        $data = $this->getAnalyticsData();
        
        $insights = AI::chat()
            ->prompt('Analyze this customer data and provide actionable insights')
            ->withData($data)
            ->system('You are a business analyst providing strategic recommendations')
            ->send();

        return [
            'insights' => $insights->content,
            'confidence_score' => $insights->metadata['confidence'] ?? null,
            'recommendations' => $this->extractRecommendations($insights->content),
        ];
    }

    private function extractRecommendations(string $content): array
    {
        // Use AI to extract structured recommendations
        $response = AI::chat()
            ->prompt('Extract 3-5 specific actionable recommendations from this text')
            ->withData(['text' => $content])
            ->responseFormat('json')
            ->send();

        return json_decode($response->content, true);
    }
}
```

## Laravel Octane 2.0 (Performance)

Laravel Octane is now mature and production-ready, delivering sub-10ms response times.

### Octane Configuration

```php
// config/octane.php
return [
    'server' => env('OCTANE_SERVER', 'roadrunner'),
    
    'servers' => [
        'roadrunner' => [
            'binary' => 'rr',
            'options' => [
                '-o', 'http.address=0.0.0.0:8000',
                '-o', 'http.pool.num_workers=8',
                '-o', 'http.pool.max_jobs=1000',
            ],
        ],
        'swoole' => [
            'options' => [
                'worker_num' => 8,
                'max_request' => 1000,
                'reload_async' => true,
            ],
        ],
    ],
    
    'watch' => [
        'app',
        'config',
        'database',
        'routes',
    ],
    
    'cache' => [
        'driver' => env('OCTANE_CACHE_DRIVER', 'redis'),
        'prefix' => 'octane',
    ],
];
```

### Performance Optimizations

```php
// Middleware for Octane optimizations
class OctaneOptimization
{
    public function handle($request, $next)
    {
        // Warm up cache in Octane context
        if (app()->bound('octane')) {
            $this->warmupCache();
        }
        
        $response = $next($request);
        
        // Add performance headers
        $response->header('X-Octane', 'true');
        $response->header('X-Response-Time', microtime(true) - LARAVEL_START);
        
        return $response;
    }

    private function warmupCache(): void
    {
        // Pre-load frequently accessed data
        Cache::remember('app:config:main', 3600, function () {
            return [
                'settings' => Setting::all(),
                'features' => Feature::all(),
            ];
        });
    }
}
```

### Memory Management

```php
// Service for managing Octane memory
class OctaneMemoryManager
{
    public static function cleanup(): void
    {
        // Clear large objects from memory
        gc_collect_cycles();
        
        // Clear specific caches
        if (memory_get_usage() > 64 * 1024 * 1024) { // 64MB
            Cache::forget(['large_dataset', 'temp_results']);
        }
    }

    public static function preloadCriticalData(): void
    {
        // Preload in memory-resident mode
        Cache::rememberMany([
            'user:permissions:' . auth()->id(),
            'app:feature_flags',
            'system:configuration',
        ], 300); // 5 minutes
    }
}
```

## Laravel Boost (AI Assistant)

Laravel Boost provides AI-powered development assistance directly in Artisan.

### AI-Powered Code Generation

```bash
# Generate controller with AI assistance
php artisan boost:make:controller UserController --ai --with-crud

# Generate API resource with validation
php artisan boost:make:resource UserResource --ai --api --validate

# Analyze and optimize existing code
php artisan boost:analyze --target=app/Http/Controllers/

# Generate tests automatically
php artisan boost:test --generate --coverage
```

### AI Code Review

```php
// Command for AI code review
class AIReviewCommand extends Command
{
    protected $signature = 'boost:review {path}';
    protected $description = 'AI-powered code review';

    public function handle()
    {
        $path = $this->argument('path');
        $code = file_get_contents($path);

        $review = AI::chat()
            ->prompt('Review this Laravel code for best practices, security, and performance')
            ->withData(['code' => $code])
            ->system('You are a senior Laravel developer conducting a code review')
            ->responseFormat('json')
            ->send();

        $data = json_decode($review->content, true);

        $this->info('Code Review Results:');
        $this->table(
            ['Category', 'Issue', 'Suggestion'],
            $data['issues']
        );

        $this->info('Security Issues:');
        foreach ($data['security'] as $issue) {
            $this->error($issue);
        }

        $this->info('Performance Suggestions:');
        foreach ($data['performance'] as $suggestion) {
            $this->line($suggestion);
        }
    }
}
```

## Laravel Pulse (Monitoring)

Laravel Pulse provides real-time application monitoring and insights.

### Pulse Configuration

```php
// config/pulse.php
return [
    'enabled' => env('PULSE_ENABLED', true),
    
    'ingest' => [
        'driver' => env('PULSE_INGEST_DRIVER', 'database'),
        'table' => 'pulse_ingest',
    ],

    'storage' => [
        'driver' => env('PULSE_STORAGE_DRIVER', 'database'),
        'connection' => env('PULSE_STORAGE_CONNECTION'),
    ],

    'recorders' => [
        // Request performance
        Laravel\Pulse\Recorders\Requests::class => [
            'sample_rate' => env('PULSE_SAMPLE_RATE', 0.01),
        ],
        
        // Database queries
        Laravel\Pulse\Recorders\CacheInteractions::class => [
            'sample_rate' => env('PULSE_CACHE_SAMPLE_RATE', 0.01),
        ],
        
        // Exception tracking
        Laravel\Pulse\Recorders\Exceptions::class => [
            'enabled' => true,
        ],
        
        // Background jobs
        Laravel\Pulse\Recorders\Jobs::class => [
            'enabled' => true,
        ],
    ],
];
```

### Custom Pulse Recorders

```php
// Custom recorder for AI usage
class AIUsageRecorder extends Recorder
{
    public function record(Collect $collect): void
    {
        if (! $collect->key) {
            return;
        }

        $collect->increment(
            key: 'ai_requests',
            value: 1,
            timestamp: now(),
            aggregate: ['count']
        );

        $collect->aggregate(
            type: 'avg',
            key: 'ai_response_time',
            value: $collect->value['response_time'] ?? 0,
            timestamp: now()
        );
    }

    public function events(): array
    {
        return [
            AIRequestProcessed::class,
        ];
    }
}
```

## Laravel Pennant (Feature Flags)

Advanced feature flag management for progressive rollouts.

### Feature Flag Configuration

```php
// App/Features.php
namespace App\Features;

use Laravel\Pennant\Feature;

class Features
{
    public static function newDashboard(): Feature
    {
        return Feature::define('new-dashboard')
            ->value(fn ($user) => $user?->email === 'admin@example.com')
            ->remember(300); // Cache for 5 minutes
    }

    public static function aiAssistant(): Feature
    {
        return Feature::define('ai-assistant')
            ->value(function ($user) {
                if (! $user) return false;
                
                // 10% of premium users
                return $user->subscription->isPremium() && 
                       $user->id % 10 === 0;
            });
    }

    public static function betaAPI(): Feature
    {
        return Feature::define('beta-api')
            ->value(fn ($user) => $user?->role === 'developer')
            ->except(fn ($user) => $user?->country === 'CN');
    }
}
```

### Using Feature Flags

```php
// In controllers
class DashboardController extends Controller
{
    public function index(Request $request)
    {
        if (Features::active('new-dashboard', $request->user())) {
            return view('dashboard.new');
        }

        return view('dashboard.legacy');
    }

    public function aiAnalyze(Request $request)
    {
        abort_unless(
            Features::active('ai-assistant', $request->user()),
            403,
            'AI Assistant feature is not enabled for your account'
        );

        // AI processing logic
        return $this->performAIAnalysis($request);
    }
}
```

## Advanced API Patterns

### GraphQL with Laravel

```php
// GraphQL schema with AI integration
class Query
{
    public function posts(array $args, AppContext $context, ResolveInfo $info): array
    {
        $user = $context->user;
        
        // AI-powered content filtering
        if (Features::active('ai-content-filter')) {
            $filteredContent = AI::chat()
                ->prompt('Filter inappropriate content from these posts')
                ->withData(['posts' => Post::all()->toArray()])
                ->send();
            
            return $filteredContent->posts;
        }

        return Post::with(['author', 'comments'])->get()->toArray();
    }
}
```

### Event-Driven Architecture

```php
// AI-powered event processing
class AIEventHandler
{
    public function handleEvent(Event $event): void
    {
        // Route to appropriate AI processor
        $processor = match($event::class) {
            UserRegistered::class => new UserOnboardingProcessor(),
            OrderPlaced::class => new OrderAnalysisProcessor(),
            SupportTicketCreated::class => new SupportTriageProcessor(),
            default => new DefaultAIProcessor(),
        };

        $processor->process($event);
    }
}

class UserOnboardingProcessor
{
    public function process(UserRegistered $event): void
    {
        $user = $event->user;
        
        // Generate personalized welcome content
        $welcome = AI::chat()
            ->prompt('Generate personalized welcome message for new user')
            ->withData([
                'user' => $user->toArray(),
                'preferences' => $user->preferences->toArray(),
            ])
            ->temperature(0.8)
            ->send();

        // Send personalized email
        Mail::to($user->email)->send(new WelcomeEmail($welcome->content));
        
        // Schedule onboarding sequence
        dispatch(new ScheduleOnboardingSequence($user));
    }
}
```

## Testing AI Features

### Testing AI Integration

```php
// Tests for AI-powered features
class AIContentGenerationTest extends TestCase
{
    public function test_ai_content_generation(): void
    {
        // Mock AI response for consistent testing
        AI::fake([
            'chat' => AI::response([
                'content' => 'Test generated content',
                'usage' => ['totalTokens' => 100],
            ]),
        ]);

        $response = $this->postJson('/api/generate-content', [
            'topic' => 'Test Topic',
            'tone' => 'professional',
        ]);

        $response->assertStatus(200);
        $response->assertJson([
            'content' => 'Test generated content',
            'tokens_used' => 100,
        ]);
    }

    public function test_ai_integration_with_real_data(): void
    {
        // Only run with AI_API_KEY is set
        $this->markTestSkippedUnless(env('AI_API_KEY'));

        $response = $this->postJson('/api/analyze-user', [
            'user_id' => 1,
        ]);

        $response->assertStatus(200);
        $this->assertArrayHasKey('insights', $response->json());
    }
}
```

### Performance Testing

```php
class OctanePerformanceTest extends TestCase
{
    public function test_octane_response_time(): void
    {
        $response = $this->get('/api/fast-endpoint');
        
        // Assert sub-10ms response in Octane
        $responseTime = $response->headers->get('X-Response-Time');
        $this->assertLessThan(0.01, $responseTime);
        
        $response->assertHeader('X-Octane', 'true');
    }

    public function test_memory_usage_optimization(): void
    {
        $initialMemory = memory_get_usage();
        
        // Perform memory-intensive operations
        $this->get('/api/process-large-dataset');
        
        $finalMemory = memory_get_usage();
        $memoryIncrease = $finalMemory - $initialMemory;
        
        // Assert memory usage is reasonable
        $this->assertLessThan(50 * 1024 * 1024, $memoryIncrease); // 50MB max
    }
}
```

## Deployment Strategies

### Octane Production Deployment

```bash
# Deploy with Octane
#!/bin/bash

# Build optimized for production
composer install --optimize-autoloader --no-dev

# Start Octane server
php artisan octane:start \
    --server=roadrunner \
    --host=0.0.0.0 \
    --port=8000 \
    --workers=8 \
    --max-requests=1000 \
    --memory-limit=256M
```

### Docker with Octane

```dockerfile
# Dockerfile for Laravel 12 + Octane
FROM php:8.2-cli

WORKDIR /var/www/html

# Install extensions
RUN docker-php-ext-install pdo_mysql redis

# Install dependencies
COPY composer.json composer.lock ./
RUN composer install --optimize-autoloader --no-dev

COPY . .

# Start Octane
CMD ["php", "artisan", "octane:start", "--server=roadrunner", "--host=0.0.0.0", "--port=8000"]
```

## Migration Guide for Laravel 13 (Q1 2026)

### Preparing for Laravel 13

1. **Upgrade PHP**: Ensure PHP 8.3+ is available
2. **Review AI SDK**: Check for breaking changes in AI SDK
3. **Test Octane**: Verify Octane compatibility
4. **Update Dependencies**: Check all packages support PHP 8.3+

### Breaking Changes Expected

- **PHP 8.3+ Required** (from 8.1+)
- **Enhanced AI SDK** with new providers
- **Updated Octane** configurations
- **New Boost commands** for AI development

## Best Practices for Laravel 12

### Do's

```php
// ✅ Use AI SDK for intelligent features
$response = AI::chat()->prompt($prompt)->send();

// ✅ Leverage Octane for performance
php artisan octane:start --server=roadrunner

// ✅ Use Pennant for feature flags
Features::active('new-feature', $user)

// ✅ Monitor with Pulse
Pulse::record('request', ['response_time' => 50]);

// ✅ Modern PHP 8.2 features
enum Status: string {
    case PENDING = 'pending';
    case APPROVED = 'approved';
}
```

### Don'ts

```php
// ❌ Don't ignore performance in production
// Always use Octane in production

// ❌ Don't skip AI SDK integration
// Leverage AI for competitive advantage

// ❌ Don't ignore feature flags
// Always use Pennant for progressive rollouts

// ❌ Don't skip monitoring
// Always enable Pulse in production
```

## Next Steps

- [Testing](./16-testing-pest-phpunit.md) - Advanced testing strategies
- [Deployment](./21-deployment.md) - Production deployment strategies
- [Microservices](./22-microservices-introduction.md) - Distributed architecture

---

[Back to Index](./README.md) | [Next: Testing](./16-testing-pest-phpunit.md)