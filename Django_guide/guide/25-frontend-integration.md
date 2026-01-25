# 25 - Frontend Integration

An API is only useful if a client can consume it. Most commonly, this client is a frontend web application built with a framework like React, Vue, or Angular. This guide covers the essential steps to connect a JavaScript frontend to your Django REST Framework API.

---

## 1. CORS: The Key to Cross-Origin Communication

For security reasons, web browsers restrict HTTP requests to a different domain (or "origin") than the one that served the frontend application. This is known as the **Same-Origin Policy**.

To allow your frontend (e.g., running on `localhost:3000`) to communicate with your API (e.g., running on `localhost:8000`), you must explicitly grant permission using **Cross-Origin Resource Sharing (CORS)**.

### Setting up `django-cors-headers`

The easiest way to manage CORS in Django is with the `django-cors-headers` package.

#### Step 1: Install the Package

```bash
pip install django-cors-headers
```
Add it to your `requirements.txt`.

#### Step 2: Configure `settings.py`

1.  Add `corsheaders` to your `INSTALLED_APPS`:

    ```python
    # settings.py
    INSTALLED_APPS = [
        # ...
        'corsheaders',
        # ...
    ]
    ```

2.  Add the `CorsMiddleware`. It should be placed as high as possible, especially before any middleware that can generate responses, like `CommonMiddleware`.

    ```python
    # settings.py
    MIDDLEWARE = [
        'corsheaders.middleware.CorsMiddleware', # Add this
        'django.middleware.security.SecurityMiddleware',
        # ...
    ]
    ```

3.  Configure which frontend origins are allowed to make requests.

    ```python
    # settings.py

    # For development, you can allow all origins:
    CORS_ALLOW_ALL_ORIGINS = True

    # Or, for production, specify a whitelist of allowed domains:
    CORS_ALLOWED_ORIGINS = [
        "https://your-frontend-domain.com",
        "http://localhost:3000",
        "http://127.0.0.1:3000",
    ]
    ```

---

## 2. Consuming the API with JavaScript

Here are basic examples of how a JavaScript frontend can interact with your API using the `fetch` API.

### Example 1: Fetching Public Data

This example shows how to make a GET request to a public endpoint, like a list of doctors.

```javascript
// Example: Fetching a list of doctors from '/api/doctors/'
async function getDoctors() {
    try {
        const response = await fetch('http://127.0.0.1:8000/api/doctors/');
        
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('Doctors:', data);
        return data;
    } catch (error) {
        console.error('Error fetching doctors:', error);
    }
}

getDoctors();
```

### Example 2: Accessing Protected Endpoints (with Authentication)

To access a protected endpoint, the frontend must include an authentication token (e.g., a JWT) in the `Authorization` header of the request.

This example assumes you have already implemented token authentication and the user has logged in and received a token.

```javascript
// Example: Creating a new booking, which requires authentication
async function createBooking(bookingData) {
    // Assume the token is stored in localStorage after login
    const token = localStorage.getItem('authToken');

    if (!token) {
        console.error('Authentication token not found.');
        return;
    }

    try {
        const response = await fetch('http://127.0.0.1:8000/api/bookings/', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                // Include the token in the Authorization header
                'Authorization': `Bearer ${token}` 
                // Note: The prefix might be 'Token ' or 'JWT ' depending on your setup
            },
            body: JSON.stringify(bookingData)
        });

        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }

        const newBooking = await response.json();
        console.log('Booking created:', newBooking);
        return newBooking;
    } catch (error) {
        console.error('Error creating booking:', error);
    }
}

// Example usage:
const newBookingData = {
    patient_id: 1,
    doctor_id: 5,
    booking_date: '2026-02-15T14:30:00Z'
};
createBooking(newBookingData);
```
With CORS configured and these frontend patterns, your API is now ready to be consumed by modern web applications.