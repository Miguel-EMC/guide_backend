# 21 - Deployment

A production deployment should be repeatable, secure, and observable. This chapter covers practical, production-grade deployment strategies including Docker, cloud platforms, zero-downtime releases, and infrastructure automation.

## Goals

- Configure Laravel for production environments
- Deploy with zero downtime and rollback capability
- Set up queues, schedulers, and long-running processes
- Automate deployments with CI/CD pipelines

## 1. Production Environment Configuration

### Environment Variables

```env
APP_NAME="Production API"
APP_ENV=production
APP_KEY=base64:your-generated-key
APP_DEBUG=false
APP_URL=https://api.example.com

LOG_CHANNEL=stack
LOG_LEVEL=warning

DB_CONNECTION=mysql
DB_HOST=db.internal
DB_PORT=3306
DB_DATABASE=app_production
DB_USERNAME=app_user
DB_PASSWORD=secure-password

CACHE_DRIVER=redis
QUEUE_CONNECTION=redis
SESSION_DRIVER=redis

REDIS_HOST=redis.internal
REDIS_PASSWORD=redis-password
REDIS_PORT=6379

MAIL_MAILER=ses
AWS_ACCESS_KEY_ID=your-key
AWS_SECRET_ACCESS_KEY=your-secret
AWS_DEFAULT_REGION=us-east-1
```

### Generate Application Key

```bash
php artisan key:generate --show
```

Store the key securely in your secrets manager, never in version control.

### Trusted Proxies

Configure trusted proxies for load balancers in `bootstrap/app.php`:

```php
->withMiddleware(function (Middleware $middleware) {
    $middleware->trustProxies(at: '*');
})
```

Or specify exact IPs:

```php
$middleware->trustProxies(at: [
    '10.0.0.0/8',
    '172.16.0.0/12',
]);
```

## 2. Storage and Permissions

### Directory Permissions

```bash
# Set ownership
chown -R www-data:www-data /var/www/app

# Storage must be writable
chmod -R 775 storage bootstrap/cache

# Restrict .env
chmod 600 .env
```

### Storage Link

```bash
php artisan storage:link
```

### Shared Storage for Multiple Servers

Use S3 or similar for shared file storage:

```php
// config/filesystems.php
'disks' => [
    's3' => [
        'driver' => 's3',
        'key' => env('AWS_ACCESS_KEY_ID'),
        'secret' => env('AWS_SECRET_ACCESS_KEY'),
        'region' => env('AWS_DEFAULT_REGION'),
        'bucket' => env('AWS_BUCKET'),
        'url' => env('AWS_URL'),
        'endpoint' => env('AWS_ENDPOINT'),
        'use_path_style_endpoint' => env('AWS_USE_PATH_STYLE_ENDPOINT', false),
    ],
],
```

## 3. Production Optimization

### Cache All Configuration

```bash
# Cache configuration files
php artisan config:cache

# Cache routes
php artisan route:cache

# Cache views
php artisan view:cache

# Cache events
php artisan event:cache

# Or run all optimizations
php artisan optimize
```

### Clear Caches (When Needed)

```bash
php artisan optimize:clear
```

### Composer Optimization

```bash
composer install --no-dev --optimize-autoloader --classmap-authoritative
```

### OPcache Configuration

```ini
; php.ini
opcache.enable=1
opcache.memory_consumption=256
opcache.interned_strings_buffer=16
opcache.max_accelerated_files=20000
opcache.validate_timestamps=0
opcache.save_comments=1
opcache.enable_file_override=1
```

Set `validate_timestamps=0` in production since files don't change between deploys.

## 4. Database Migrations

### Safe Migration Commands

```bash
# Run migrations in production
php artisan migrate --force

# Check pending migrations without running
php artisan migrate:status
```

### Migration Best Practices

```php
// Create non-blocking index
Schema::table('orders', function (Blueprint $table) {
    $table->index('status', 'orders_status_index');
});

// Add nullable column first, then populate
Schema::table('users', function (Blueprint $table) {
    $table->string('timezone')->nullable()->after('email');
});

// Separate job to backfill data
// Then make column non-nullable in a later migration
```

### Rollback Strategy

```bash
# Rollback last batch
php artisan migrate:rollback --step=1

# Check what would be rolled back
php artisan migrate:status
```

## 5. Health Check Endpoints

### Built-in Health Route

Configure in `bootstrap/app.php`:

```php
->withRouting(
    web: __DIR__.'/../routes/web.php',
    api: __DIR__.'/../routes/api.php',
    commands: __DIR__.'/../routes/console.php',
    health: '/up',
)
```

### Custom Health Check

```php
// routes/api.php
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Route;

Route::get('/health', function () {
    $checks = [];

    // Database check
    try {
        DB::connection()->getPdo();
        $checks['database'] = 'ok';
    } catch (\Exception $e) {
        $checks['database'] = 'failed';
    }

    // Cache check
    try {
        Cache::store()->put('health_check', true, 10);
        Cache::store()->get('health_check');
        $checks['cache'] = 'ok';
    } catch (\Exception $e) {
        $checks['cache'] = 'failed';
    }

    // Queue check
    try {
        $checks['queue'] = Cache::get('queue:heartbeat', 'unknown');
    } catch (\Exception $e) {
        $checks['queue'] = 'failed';
    }

    $healthy = !in_array('failed', $checks);

    return response()->json([
        'status' => $healthy ? 'healthy' : 'unhealthy',
        'checks' => $checks,
        'timestamp' => now()->toIso8601String(),
    ], $healthy ? 200 : 503);
});
```

### Liveness vs Readiness

```php
// Liveness: Is the process alive?
Route::get('/healthz', fn () => response()->json(['status' => 'ok']));

// Readiness: Can it accept traffic?
Route::get('/ready', function () {
    try {
        DB::connection()->getPdo();
        return response()->json(['status' => 'ready']);
    } catch (\Exception $e) {
        return response()->json(['status' => 'not_ready'], 503);
    }
});
```

## 6. Queue Workers and Supervisord

### Supervisor Configuration

```ini
; /etc/supervisor/conf.d/laravel-worker.conf
[program:laravel-worker]
process_name=%(program_name)s_%(process_num)02d
command=php /var/www/app/artisan queue:work redis --sleep=3 --tries=3 --max-time=3600
autostart=true
autorestart=true
stopasgroup=true
killasgroup=true
user=www-data
numprocs=4
redirect_stderr=true
stdout_logfile=/var/www/app/storage/logs/worker.log
stopwaitsecs=3600
```

### Restart Workers After Deploy

```bash
php artisan queue:restart
```

### Horizon (Redis Queue Dashboard)

```bash
composer require laravel/horizon
php artisan horizon:install
php artisan horizon
```

Supervisor config for Horizon:

```ini
[program:horizon]
process_name=%(program_name)s
command=php /var/www/app/artisan horizon
autostart=true
autorestart=true
user=www-data
redirect_stderr=true
stdout_logfile=/var/www/app/storage/logs/horizon.log
stopwaitsecs=3600
```

## 7. Scheduler

### Cron Entry

```bash
* * * * * cd /var/www/app && php artisan schedule:run >> /dev/null 2>&1
```

### Supervisor for Schedule Worker

```ini
[program:scheduler]
process_name=%(program_name)s
command=php /var/www/app/artisan schedule:work
autostart=true
autorestart=true
user=www-data
redirect_stderr=true
stdout_logfile=/var/www/app/storage/logs/scheduler.log
```

## 8. Docker Deployment

### Production Dockerfile

```dockerfile
FROM php:8.3-fpm-alpine AS base

# Install system dependencies
RUN apk add --no-cache \
    nginx \
    supervisor \
    libpng-dev \
    libzip-dev \
    oniguruma-dev \
    icu-dev \
    && docker-php-ext-install \
    pdo_mysql \
    mbstring \
    gd \
    zip \
    intl \
    opcache \
    pcntl

# Install Redis extension
RUN apk add --no-cache --virtual .build-deps $PHPIZE_DEPS \
    && pecl install redis \
    && docker-php-ext-enable redis \
    && apk del .build-deps

# Copy PHP configuration
COPY docker/php/php.ini /usr/local/etc/php/conf.d/app.ini
COPY docker/php/opcache.ini /usr/local/etc/php/conf.d/opcache.ini

WORKDIR /var/www/app

# Composer stage
FROM composer:2 AS composer
WORKDIR /var/www/app
COPY composer.json composer.lock ./
RUN composer install --no-dev --no-scripts --no-autoloader --prefer-dist

COPY . .
RUN composer dump-autoload --optimize --classmap-authoritative

# Final stage
FROM base AS production

# Copy application
COPY --from=composer /var/www/app /var/www/app

# Copy nginx config
COPY docker/nginx/default.conf /etc/nginx/http.d/default.conf

# Copy supervisor config
COPY docker/supervisor/supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Set permissions
RUN chown -R www-data:www-data /var/www/app \
    && chmod -R 775 storage bootstrap/cache

# Optimize Laravel
RUN php artisan config:cache \
    && php artisan route:cache \
    && php artisan view:cache

EXPOSE 80

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
```

### Nginx Configuration

```nginx
# docker/nginx/default.conf
server {
    listen 80;
    server_name _;
    root /var/www/app/public;
    index index.php;

    client_max_body_size 100M;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    location ~ \.php$ {
        fastcgi_pass 127.0.0.1:9000;
        fastcgi_param SCRIPT_FILENAME $realpath_root$fastcgi_script_name;
        include fastcgi_params;
        fastcgi_read_timeout 300;
    }

    location ~ /\.(?!well-known).* {
        deny all;
    }

    # Health check endpoint
    location = /up {
        access_log off;
        try_files $uri /index.php?$query_string;
    }
}
```

### Docker Compose for Production

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
      target: production
    restart: unless-stopped
    environment:
      - APP_ENV=production
    depends_on:
      - redis
      - mysql
    networks:
      - app-network

  worker:
    build:
      context: .
      dockerfile: Dockerfile
      target: production
    restart: unless-stopped
    command: php artisan queue:work --sleep=3 --tries=3 --max-time=3600
    environment:
      - APP_ENV=production
    depends_on:
      - redis
      - mysql
    networks:
      - app-network

  scheduler:
    build:
      context: .
      dockerfile: Dockerfile
      target: production
    restart: unless-stopped
    command: php artisan schedule:work
    environment:
      - APP_ENV=production
    depends_on:
      - redis
      - mysql
    networks:
      - app-network

  redis:
    image: redis:7-alpine
    restart: unless-stopped
    volumes:
      - redis-data:/data
    networks:
      - app-network

  mysql:
    image: mysql:8.0
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: ${DB_DATABASE}
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - app-network

volumes:
  redis-data:
  mysql-data:

networks:
  app-network:
    driver: bridge
```

## 9. Kubernetes Deployment

### Deployment Manifest

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: laravel-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: laravel-api
  template:
    metadata:
      labels:
        app: laravel-api
    spec:
      containers:
        - name: app
          image: your-registry/laravel-app:latest
          ports:
            - containerPort: 80
          envFrom:
            - secretRef:
                name: laravel-secrets
            - configMapRef:
                name: laravel-config
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /healthz
              port: 80
            initialDelaySeconds: 10
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /ready
              port: 80
            initialDelaySeconds: 5
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: laravel-api
spec:
  selector:
    app: laravel-api
  ports:
    - port: 80
      targetPort: 80
  type: ClusterIP
```

### Worker Deployment

```yaml
# k8s/worker.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: laravel-worker
spec:
  replicas: 2
  selector:
    matchLabels:
      app: laravel-worker
  template:
    metadata:
      labels:
        app: laravel-worker
    spec:
      containers:
        - name: worker
          image: your-registry/laravel-app:latest
          command: ["php", "artisan", "queue:work", "--sleep=3", "--tries=3"]
          envFrom:
            - secretRef:
                name: laravel-secrets
          resources:
            requests:
              memory: "128Mi"
              cpu: "100m"
            limits:
              memory: "256Mi"
              cpu: "200m"
```

### Horizontal Pod Autoscaler

```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: laravel-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: laravel-api
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

## 10. Zero-Downtime Deployments

### Maintenance Mode

```bash
# Enter maintenance mode with custom view
php artisan down --render="errors::503" --retry=60

# Allow specific IPs
php artisan down --allow=192.168.1.0/24

# Exit maintenance mode
php artisan up
```

### Blue-Green Deployment Script

```bash
#!/bin/bash
set -e

DEPLOY_DIR="/var/www"
CURRENT_LINK="$DEPLOY_DIR/current"
RELEASES_DIR="$DEPLOY_DIR/releases"
SHARED_DIR="$DEPLOY_DIR/shared"
RELEASE=$(date +%Y%m%d%H%M%S)
RELEASE_DIR="$RELEASES_DIR/$RELEASE"

echo "Deploying release: $RELEASE"

# Clone repository
git clone --depth 1 git@github.com:org/repo.git "$RELEASE_DIR"
cd "$RELEASE_DIR"

# Install dependencies
composer install --no-dev --optimize-autoloader --classmap-authoritative

# Link shared directories
ln -nfs "$SHARED_DIR/.env" "$RELEASE_DIR/.env"
ln -nfs "$SHARED_DIR/storage" "$RELEASE_DIR/storage"

# Run migrations
php artisan migrate --force

# Cache configuration
php artisan config:cache
php artisan route:cache
php artisan view:cache

# Switch symlink atomically
ln -nfs "$RELEASE_DIR" "$CURRENT_LINK"

# Restart queue workers
php artisan queue:restart

# Reload PHP-FPM
sudo systemctl reload php8.3-fpm

# Clean old releases (keep last 5)
cd "$RELEASES_DIR"
ls -t | tail -n +6 | xargs rm -rf

echo "Deployment complete!"
```

### Rollback Script

```bash
#!/bin/bash
set -e

DEPLOY_DIR="/var/www"
RELEASES_DIR="$DEPLOY_DIR/releases"
CURRENT_LINK="$DEPLOY_DIR/current"

# Get previous release
PREVIOUS=$(ls -t "$RELEASES_DIR" | sed -n '2p')

if [ -z "$PREVIOUS" ]; then
    echo "No previous release found!"
    exit 1
fi

echo "Rolling back to: $PREVIOUS"

# Switch symlink
ln -nfs "$RELEASES_DIR/$PREVIOUS" "$CURRENT_LINK"

# Restart services
php artisan queue:restart
sudo systemctl reload php8.3-fpm

echo "Rollback complete!"
```

## 11. Web Server Configuration

### Nginx with PHP-FPM

```nginx
# /etc/nginx/sites-available/laravel.conf
server {
    listen 80;
    server_name api.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate /etc/ssl/certs/api.example.com.crt;
    ssl_certificate_key /etc/ssl/private/api.example.com.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers off;

    root /var/www/current/public;
    index index.php;

    client_max_body_size 100M;

    # Gzip compression
    gzip on;
    gzip_types application/json text/plain application/xml;
    gzip_min_length 1000;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    location ~ \.php$ {
        fastcgi_pass unix:/var/run/php/php8.3-fpm.sock;
        fastcgi_param SCRIPT_FILENAME $realpath_root$fastcgi_script_name;
        include fastcgi_params;
        fastcgi_read_timeout 300;
        fastcgi_buffer_size 128k;
        fastcgi_buffers 4 256k;
    }

    location ~ /\.(?!well-known).* {
        deny all;
    }

    # Static assets caching
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff2?)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

### PHP-FPM Pool Configuration

```ini
; /etc/php/8.3/fpm/pool.d/www.conf
[www]
user = www-data
group = www-data

listen = /var/run/php/php8.3-fpm.sock
listen.owner = www-data
listen.group = www-data

pm = dynamic
pm.max_children = 50
pm.start_servers = 10
pm.min_spare_servers = 5
pm.max_spare_servers = 20
pm.max_requests = 1000

request_terminate_timeout = 300
catch_workers_output = yes

; Environment variables
env[APP_ENV] = production
```

## 12. CI/CD Pipeline

### GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: password
          MYSQL_DATABASE: testing
        ports:
          - 3306:3306
        options: --health-cmd="mysqladmin ping" --health-interval=10s --health-timeout=5s --health-retries=3

    steps:
      - uses: actions/checkout@v4

      - name: Setup PHP
        uses: shivammathur/setup-php@v2
        with:
          php-version: '8.3'
          extensions: mbstring, pdo_mysql, redis
          coverage: xdebug

      - name: Install dependencies
        run: composer install --prefer-dist --no-progress

      - name: Run tests
        run: php artisan test --parallel
        env:
          DB_CONNECTION: mysql
          DB_HOST: 127.0.0.1
          DB_DATABASE: testing
          DB_USERNAME: root
          DB_PASSWORD: password

  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v4

      - name: Deploy to server
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /var/www/app
            ./deploy.sh
```

### GitLab CI

```yaml
# .gitlab-ci.yml
stages:
  - test
  - build
  - deploy

variables:
  MYSQL_ROOT_PASSWORD: password
  MYSQL_DATABASE: testing

test:
  stage: test
  image: php:8.3-cli
  services:
    - mysql:8.0
  before_script:
    - apt-get update && apt-get install -y git unzip libpng-dev
    - docker-php-ext-install pdo_mysql gd
    - curl -sS https://getcomposer.org/installer | php
    - php composer.phar install --prefer-dist --no-progress
  script:
    - php artisan test
  only:
    - merge_requests
    - main

build:
  stage: build
  image: docker:24
  services:
    - docker:24-dind
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
  only:
    - main

deploy:
  stage: deploy
  image: alpine:latest
  before_script:
    - apk add --no-cache openssh-client
    - eval $(ssh-agent -s)
    - echo "$SSH_PRIVATE_KEY" | ssh-add -
  script:
    - ssh $SERVER_USER@$SERVER_HOST "cd /var/www/app && ./deploy.sh"
  only:
    - main
  when: manual
```

## 13. Monitoring and Alerting

### Application Performance Monitoring

```php
// config/services.php
'sentry' => [
    'dsn' => env('SENTRY_LARAVEL_DSN'),
    'traces_sample_rate' => env('SENTRY_TRACES_SAMPLE_RATE', 0.1),
],
```

### Custom Metrics

```php
// app/Providers/MetricsServiceProvider.php
namespace App\Providers;

use Illuminate\Support\Facades\DB;
use Illuminate\Support\ServiceProvider;
use Prometheus\CollectorRegistry;

class MetricsServiceProvider extends ServiceProvider
{
    public function boot(CollectorRegistry $registry): void
    {
        $histogram = $registry->getOrRegisterHistogram(
            'app',
            'db_query_duration_seconds',
            'Database query duration',
            ['query_type']
        );

        DB::listen(function ($query) use ($histogram) {
            $type = strtoupper(strtok($query->sql, ' '));
            $histogram->observe($query->time / 1000, [$type]);
        });
    }
}
```

### Log Aggregation

```php
// config/logging.php
'channels' => [
    'stack' => [
        'driver' => 'stack',
        'channels' => ['daily', 'stderr'],
    ],

    'stderr' => [
        'driver' => 'monolog',
        'level' => env('LOG_LEVEL', 'warning'),
        'handler' => StreamHandler::class,
        'formatter' => JsonFormatter::class,
        'with' => [
            'stream' => 'php://stderr',
        ],
    ],
],
```

## 14. Security Checklist

```markdown
## Pre-Deployment Security Checklist

### Environment
- [ ] APP_DEBUG=false
- [ ] APP_ENV=production
- [ ] Unique APP_KEY generated and stored securely
- [ ] .env file not in version control
- [ ] .env has restrictive permissions (600)

### Database
- [ ] Database credentials use dedicated user (not root)
- [ ] Database user has minimal required permissions
- [ ] SSL connection to database enabled

### Application
- [ ] CSRF protection enabled
- [ ] XSS protection headers configured
- [ ] Rate limiting configured
- [ ] Input validation on all endpoints
- [ ] SQL injection protection (parameterized queries)
- [ ] Sensitive data encrypted at rest

### Infrastructure
- [ ] HTTPS enforced
- [ ] TLS 1.2+ only
- [ ] Firewall configured
- [ ] SSH key authentication only
- [ ] Regular security updates scheduled

### Monitoring
- [ ] Error tracking configured
- [ ] Security alerts enabled
- [ ] Log retention policy defined
- [ ] Backup verification tested
```

## Tips

- Never deploy with `APP_DEBUG=true`.
- Store secrets in a secrets manager, not in `.env` files.
- Run smoke tests after every deployment.
- Keep rollback scripts ready and tested.
- Use blue-green or rolling deployments for zero downtime.
- Monitor deployment metrics (deploy frequency, failure rate, rollback rate).

---

[Previous: Package Development](./20-package-development.md) | [Back to Index](./README.md) | [Next: Microservices Introduction ->](./22-microservices-introduction.md)
