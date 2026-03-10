# Commodity Monitor API - Complete Postman Testing Guide

## Table of Contents
1. [Setup & Configuration](#setup--configuration)
2. [Authentication Endpoints](#authentication-endpoints)
3. [City Management Endpoints](#city-management-endpoints)
4. [Market Management Endpoints](#market-management-endpoints)
5. [Commodity Management Endpoints](#commodity-management-endpoints)
6. [Price Record Management Endpoints](#price-record-management-endpoints)
7. [Analytics Endpoints](#analytics-endpoints)

---

## Setup & Configuration

### Base URL
```
http://localhost:8080
```

### Environment Variables (Recommended)
Create these variables in Postman for easier testing:
- `base_url`: `http://localhost:8080`
- `jwt_token`: (will be set after login)

### Global Headers
Most endpoints require:
- `Content-Type`: `application/json`
- `Authorization`: `Bearer {{jwt_token}}` (for protected endpoints)

---

## Authentication Endpoints

### 1. Login (Get JWT Token)

**Purpose**: Authenticate and obtain a JWT token for accessing protected endpoints.

**Endpoint**: `POST /api/v1/auth/login`

**URL**: `{{base_url}}/api/v1/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "admin",
    "role": "ADMIN"
  },
  "timestamp": "2026-03-10T12:00:00Z"
}
```

**Postman Testing Steps**:
1. Create a new POST request
2. Enter the URL: `{{base_url}}/api/v1/auth/login`
3. Go to Headers tab, add `Content-Type: application/json`
4. Go to Body tab, select "raw" and "JSON"
5. Paste the request body
6. Click "Send"
7. In the "Tests" tab, add this script to auto-save the token:
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("jwt_token", jsonData.data.token);
}
```
8. The token will be automatically saved for subsequent requests

**Error Responses**:
- `401 Unauthorized`: Invalid credentials
- `400 Bad Request`: Missing or invalid fields

---

### 2. Register New User (Admin Only)

**Purpose**: Create a new user account (requires ADMIN role).

**Endpoint**: `POST /api/v1/auth/register`

**URL**: `{{base_url}}/api/v1/auth/register`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Request Body**:
```json
{
  "username": "john_viewer",
  "password": "password123",
  "email": "john@example.com",
  "role": "VIEWER"
}
```

**Alternative Request Body** (Create Admin):
```json
{
  "username": "super_admin",
  "password": "securepass456",
  "email": "admin@commoditygh.com",
  "role": "ADMIN"
}
```

**Example Response** (201 Created):
```json
{
  "status": "success",
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "john_viewer",
    "role": "VIEWER"
  },
  "timestamp": "2026-03-10T12:05:00Z"
}
```

**Postman Testing Steps**:
1. Create a new POST request
2. Enter the URL: `{{base_url}}/api/v1/auth/register`
3. Go to Headers tab, add:
   - `Content-Type: application/json`
   - `Authorization: Bearer {{jwt_token}}`
4. Go to Body tab, select "raw" and "JSON"
5. Paste the request body
6. Click "Send"

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `400 Bad Request`: Validation errors (username too short, invalid email, etc.)
- `409 Conflict`: Username or email already exists

---

## City Management Endpoints

### 3. Get All Cities

**Purpose**: Retrieve a list of all cities in Ghana.

**Endpoint**: `GET /api/v1/cities`

**URL**: `{{base_url}}/api/v1/cities`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Query Parameters**: None

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "id": 1,
      "name": "Accra",
      "region": "Greater Accra",
      "createdAt": "2026-03-01T10:00:00Z"
    },
    {
      "id": 2,
      "name": "Kumasi",
      "region": "Ashanti",
      "createdAt": "2026-03-01T10:00:00Z"
    }
  ],
  "timestamp": "2026-03-10T12:10:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/cities`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

---

### 4. Get City by ID

**Purpose**: Retrieve details of a specific city.

**Endpoint**: `GET /api/v1/cities/{id}`

**URL**: `{{base_url}}/api/v1/cities/1`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: City ID (e.g., 1)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": {
    "id": 1,
    "name": "Accra",
    "region": "Greater Accra",
    "createdAt": "2026-03-01T10:00:00Z"
  },
  "timestamp": "2026-03-10T12:15:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/cities/1`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

**Error Responses**:
- `404 Not Found`: City with specified ID does not exist

---

### 5. Get Cities by Region

**Purpose**: Retrieve all cities in a specific region.

**Endpoint**: `GET /api/v1/cities/region/{region}`

**URL**: `{{base_url}}/api/v1/cities/region/Ashanti`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `region`: Region name (e.g., "Ashanti", "Greater Accra", "Northern")

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "id": 2,
      "name": "Kumasi",
      "region": "Ashanti",
      "createdAt": "2026-03-01T10:00:00Z"
    }
  ],
  "timestamp": "2026-03-10T12:20:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/cities/region/Ashanti`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

---

### 6. Create New City (Admin Only)

**Purpose**: Add a new city to the system.

**Endpoint**: `POST /api/v1/cities`

**URL**: `{{base_url}}/api/v1/cities`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Request Body**:
```json
{
  "name": "Tamale",
  "region": "Northern"
}
```

**Example Response** (201 Created):
```json
{
  "status": "success",
  "message": "City created successfully",
  "data": {
    "id": 3,
    "name": "Tamale",
    "region": "Northern",
    "createdAt": "2026-03-10T12:25:00Z"
  },
  "timestamp": "2026-03-10T12:25:00Z"
}
```

**Postman Testing Steps**:
1. Create a new POST request
2. Enter the URL: `{{base_url}}/api/v1/cities`
3. Go to Headers tab, add:
   - `Content-Type: application/json`
   - `Authorization: Bearer {{jwt_token}}`
4. Go to Body tab, select "raw" and "JSON"
5. Paste the request body
6. Click "Send"

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `400 Bad Request`: Validation errors (missing name or region)
- `409 Conflict`: City name already exists

---

### 7. Update City (Admin Only)

**Purpose**: Update an existing city's information.

**Endpoint**: `PUT /api/v1/cities/{id}`

**URL**: `{{base_url}}/api/v1/cities/3`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: City ID to update (e.g., 3)

**Request Body**:
```json
{
  "name": "Tamale Metropolitan",
  "region": "Northern Region"
}
```

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "City updated successfully",
  "data": {
    "id": 3,
    "name": "Tamale Metropolitan",
    "region": "Northern Region",
    "createdAt": "2026-03-10T12:25:00Z"
  },
  "timestamp": "2026-03-10T12:30:00Z"
}
```

**Postman Testing Steps**:
1. Create a new PUT request
2. Enter the URL: `{{base_url}}/api/v1/cities/3`
3. Go to Headers tab, add:
   - `Content-Type: application/json`
   - `Authorization: Bearer {{jwt_token}}`
4. Go to Body tab, select "raw" and "JSON"
5. Paste the request body
6. Click "Send"

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `404 Not Found`: City with specified ID does not exist
- `400 Bad Request`: Validation errors

---

### 8. Delete City (Admin Only)

**Purpose**: Remove a city from the system.

**Endpoint**: `DELETE /api/v1/cities/{id}`

**URL**: `{{base_url}}/api/v1/cities/3`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: City ID to delete (e.g., 3)

**Example Response** (204 No Content):
```
(Empty response body)
```

**Postman Testing Steps**:
1. Create a new DELETE request
2. Enter the URL: `{{base_url}}/api/v1/cities/3`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"
5. Verify status code is 204

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `404 Not Found`: City with specified ID does not exist

---

## Market Management Endpoints

### 9. Get All Markets

**Purpose**: Retrieve a list of all markets.

**Endpoint**: `GET /api/v1/markets`

**URL**: `{{base_url}}/api/v1/markets`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "id": 1,
      "name": "Makola Market",
      "cityId": 1,
      "cityName": "Accra",
      "createdAt": "2026-03-01T10:00:00Z"
    },
    {
      "id": 2,
      "name": "Kejetia Market",
      "cityId": 2,
      "cityName": "Kumasi",
      "createdAt": "2026-03-01T10:00:00Z"
    }
  ],
  "timestamp": "2026-03-10T12:35:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/markets`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

---

### 10. Get Market by ID

**Purpose**: Retrieve details of a specific market.

**Endpoint**: `GET /api/v1/markets/{id}`

**URL**: `{{base_url}}/api/v1/markets/1`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: Market ID (e.g., 1)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": {
    "id": 1,
    "name": "Makola Market",
    "cityId": 1,
    "cityName": "Accra",
    "createdAt": "2026-03-01T10:00:00Z"
  },
  "timestamp": "2026-03-10T12:40:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/markets/1`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

**Error Responses**:
- `404 Not Found`: Market with specified ID does not exist

---

### 11. Get Markets by City

**Purpose**: Retrieve all markets in a specific city.

**Endpoint**: `GET /api/v1/markets/city/{cityId}`

**URL**: `{{base_url}}/api/v1/markets/city/1`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `cityId`: City ID (e.g., 1)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "id": 1,
      "name": "Makola Market",
      "cityId": 1,
      "cityName": "Accra",
      "createdAt": "2026-03-01T10:00:00Z"
    },
    {
      "id": 3,
      "name": "Kaneshie Market",
      "cityId": 1,
      "cityName": "Accra",
      "createdAt": "2026-03-01T10:00:00Z"
    }
  ],
  "timestamp": "2026-03-10T12:45:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/markets/city/1`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

---

### 12. Create New Market (Admin Only)

**Purpose**: Add a new market to the system.

**Endpoint**: `POST /api/v1/markets`

**URL**: `{{base_url}}/api/v1/markets`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Request Body**:
```json
{
  "name": "Agbogbloshie Market",
  "cityId": 1
}
```

**Example Response** (201 Created):
```json
{
  "status": "success",
  "message": "Market created successfully",
  "data": {
    "id": 4,
    "name": "Agbogbloshie Market",
    "cityId": 1,
    "cityName": "Accra",
    "createdAt": "2026-03-10T12:50:00Z"
  },
  "timestamp": "2026-03-10T12:50:00Z"
}
```

**Postman Testing Steps**:
1. Create a new POST request
2. Enter the URL: `{{base_url}}/api/v1/markets`
3. Go to Headers tab, add:
   - `Content-Type: application/json`
   - `Authorization: Bearer {{jwt_token}}`
4. Go to Body tab, select "raw" and "JSON"
5. Paste the request body
6. Click "Send"

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `400 Bad Request`: Validation errors (missing name or invalid cityId)
- `404 Not Found`: City with specified cityId does not exist

---

### 13. Update Market (Admin Only)

**Purpose**: Update an existing market's information.

**Endpoint**: `PUT /api/v1/markets/{id}`

**URL**: `{{base_url}}/api/v1/markets/4`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: Market ID to update (e.g., 4)

**Request Body**:
```json
{
  "name": "Agbogbloshie Central Market",
  "cityId": 1
}
```

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Market updated successfully",
  "data": {
    "id": 4,
    "name": "Agbogbloshie Central Market",
    "cityId": 1,
    "cityName": "Accra",
    "createdAt": "2026-03-10T12:50:00Z"
  },
  "timestamp": "2026-03-10T12:55:00Z"
}
```

**Postman Testing Steps**:
1. Create a new PUT request
2. Enter the URL: `{{base_url}}/api/v1/markets/4`
3. Go to Headers tab, add:
   - `Content-Type: application/json`
   - `Authorization: Bearer {{jwt_token}}`
4. Go to Body tab, select "raw" and "JSON"
5. Paste the request body
6. Click "Send"

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `404 Not Found`: Market with specified ID does not exist
- `400 Bad Request`: Validation errors

---

### 14. Delete Market (Admin Only)

**Purpose**: Remove a market from the system.

**Endpoint**: `DELETE /api/v1/markets/{id}`

**URL**: `{{base_url}}/api/v1/markets/4`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: Market ID to delete (e.g., 4)

**Example Response** (204 No Content):
```
(Empty response body)
```

**Postman Testing Steps**:
1. Create a new DELETE request
2. Enter the URL: `{{base_url}}/api/v1/markets/4`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"
5. Verify status code is 204

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `404 Not Found`: Market with specified ID does not exist

---

## Commodity Management Endpoints

### 15. Get All Commodities

**Purpose**: Retrieve a list of all commodities being monitored.

**Endpoint**: `GET /api/v1/commodities`

**URL**: `{{base_url}}/api/v1/commodities`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "id": 1,
      "name": "Rice",
      "category": "Grains",
      "unit": "kg",
      "createdAt": "2026-03-01T10:00:00Z"
    },
    {
      "id": 2,
      "name": "Tomatoes",
      "category": "Vegetables",
      "unit": "kg",
      "createdAt": "2026-03-01T10:00:00Z"
    },
    {
      "id": 3,
      "name": "Cooking Oil",
      "category": "Oils",
      "unit": "liter",
      "createdAt": "2026-03-01T10:00:00Z"
    }
  ],
  "timestamp": "2026-03-10T13:00:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/commodities`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

---

### 16. Get Commodity by ID

**Purpose**: Retrieve details of a specific commodity.

**Endpoint**: `GET /api/v1/commodities/{id}`

**URL**: `{{base_url}}/api/v1/commodities/1`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: Commodity ID (e.g., 1)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": {
    "id": 1,
    "name": "Rice",
    "category": "Grains",
    "unit": "kg",
    "createdAt": "2026-03-01T10:00:00Z"
  },
  "timestamp": "2026-03-10T13:05:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/commodities/1`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

**Error Responses**:
- `404 Not Found`: Commodity with specified ID does not exist

---

### 17. Get Commodities by Category

**Purpose**: Retrieve all commodities in a specific category.

**Endpoint**: `GET /api/v1/commodities/category/{category}`

**URL**: `{{base_url}}/api/v1/commodities/category/Grains`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `category`: Category name (e.g., "Grains", "Vegetables", "Oils")

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "id": 1,
      "name": "Rice",
      "category": "Grains",
      "unit": "kg",
      "createdAt": "2026-03-01T10:00:00Z"
    },
    {
      "id": 4,
      "name": "Maize",
      "category": "Grains",
      "unit": "kg",
      "createdAt": "2026-03-01T10:00:00Z"
    }
  ],
  "timestamp": "2026-03-10T13:10:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/commodities/category/Grains`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

---

### 18. Create New Commodity (Admin Only)

**Purpose**: Add a new commodity to the monitoring system.

**Endpoint**: `POST /api/v1/commodities`

**URL**: `{{base_url}}/api/v1/commodities`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Request Body**:
```json
{
  "name": "Onions",
  "category": "Vegetables",
  "unit": "kg"
}
```

**Alternative Examples**:
```json
{
  "name": "Palm Oil",
  "category": "Oils",
  "unit": "liter"
}
```

```json
{
  "name": "Yam",
  "category": "Tubers",
  "unit": "tuber"
}
```

**Example Response** (201 Created):
```json
{
  "status": "success",
  "message": "Commodity created successfully",
  "data": {
    "id": 5,
    "name": "Onions",
    "category": "Vegetables",
    "unit": "kg",
    "createdAt": "2026-03-10T13:15:00Z"
  },
  "timestamp": "2026-03-10T13:15:00Z"
}
```

**Postman Testing Steps**:
1. Create a new POST request
2. Enter the URL: `{{base_url}}/api/v1/commodities`
3. Go to Headers tab, add:
   - `Content-Type: application/json`
   - `Authorization: Bearer {{jwt_token}}`
4. Go to Body tab, select "raw" and "JSON"
5. Paste the request body
6. Click "Send"

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `400 Bad Request`: Validation errors (missing name, category, or unit)
- `409 Conflict`: Commodity name already exists

---

### 19. Update Commodity (Admin Only)

**Purpose**: Update an existing commodity's information.

**Endpoint**: `PUT /api/v1/commodities/{id}`

**URL**: `{{base_url}}/api/v1/commodities/5`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: Commodity ID to update (e.g., 5)

**Request Body**:
```json
{
  "name": "Red Onions",
  "category": "Vegetables",
  "unit": "kg"
}
```

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Commodity updated successfully",
  "data": {
    "id": 5,
    "name": "Red Onions",
    "category": "Vegetables",
    "unit": "kg",
    "createdAt": "2026-03-10T13:15:00Z"
  },
  "timestamp": "2026-03-10T13:20:00Z"
}
```

**Postman Testing Steps**:
1. Create a new PUT request
2. Enter the URL: `{{base_url}}/api/v1/commodities/5`
3. Go to Headers tab, add:
   - `Content-Type: application/json`
   - `Authorization: Bearer {{jwt_token}}`
4. Go to Body tab, select "raw" and "JSON"
5. Paste the request body
6. Click "Send"

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `404 Not Found`: Commodity with specified ID does not exist
- `400 Bad Request`: Validation errors

---

### 20. Delete Commodity (Admin Only)

**Purpose**: Remove a commodity from the system.

**Endpoint**: `DELETE /api/v1/commodities/{id}`

**URL**: `{{base_url}}/api/v1/commodities/5`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: Commodity ID to delete (e.g., 5)

**Example Response** (204 No Content):
```
(Empty response body)
```

**Postman Testing Steps**:
1. Create a new DELETE request
2. Enter the URL: `{{base_url}}/api/v1/commodities/5`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"
5. Verify status code is 204

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `404 Not Found`: Commodity with specified ID does not exist

---

## Price Record Management Endpoints

### 21. Get All Price Records (Paginated)

**Purpose**: Retrieve historical price records with pagination support.

**Endpoint**: `GET /api/v1/price-records`

**URL**: `{{base_url}}/api/v1/price-records?page=0&size=10&sort=recordedDate,desc`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Number of records per page (default: 20)
- `sort`: Sort field and direction (e.g., "recordedDate,desc", "price,asc")

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": {
    "content": [
      {
        "id": 1,
        "commodityId": 1,
        "commodityName": "Rice",
        "marketId": 1,
        "marketName": "Makola Market",
        "price": 45.50,
        "recordedDate": "2026-03-09",
        "source": "Market Survey",
        "createdAt": "2026-03-09T14:00:00Z"
      },
      {
        "id": 2,
        "commodityId": 2,
        "commodityName": "Tomatoes",
        "marketId": 1,
        "marketName": "Makola Market",
        "price": 12.00,
        "recordedDate": "2026-03-09",
        "source": "Market Survey",
        "createdAt": "2026-03-09T14:00:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 50,
    "totalPages": 5,
    "last": false
  },
  "timestamp": "2026-03-10T13:25:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/price-records`
3. Go to Params tab, add query parameters:
   - `page`: 0
   - `size`: 10
   - `sort`: recordedDate,desc
4. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
5. Click "Send"

---

### 22. Get Price Record by ID

**Purpose**: Retrieve details of a specific price record.

**Endpoint**: `GET /api/v1/price-records/{id}`

**URL**: `{{base_url}}/api/v1/price-records/1`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: Price record ID (e.g., 1)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": {
    "id": 1,
    "commodityId": 1,
    "commodityName": "Rice",
    "marketId": 1,
    "marketName": "Makola Market",
    "price": 45.50,
    "recordedDate": "2026-03-09",
    "source": "Market Survey",
    "createdAt": "2026-03-09T14:00:00Z"
  },
  "timestamp": "2026-03-10T13:30:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/price-records/1`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

**Error Responses**:
- `404 Not Found`: Price record with specified ID does not exist

---

### 23. Get Price Records by Commodity

**Purpose**: Retrieve all price records for a specific commodity.

**Endpoint**: `GET /api/v1/price-records/commodity/{commodityId}`

**URL**: `{{base_url}}/api/v1/price-records/commodity/1`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `commodityId`: Commodity ID (e.g., 1)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "id": 1,
      "commodityId": 1,
      "commodityName": "Rice",
      "marketId": 1,
      "marketName": "Makola Market",
      "price": 45.50,
      "recordedDate": "2026-03-09",
      "source": "Market Survey",
      "createdAt": "2026-03-09T14:00:00Z"
    },
    {
      "id": 5,
      "commodityId": 1,
      "commodityName": "Rice",
      "marketId": 2,
      "marketName": "Kejetia Market",
      "price": 43.00,
      "recordedDate": "2026-03-09",
      "source": "Market Survey",
      "createdAt": "2026-03-09T14:00:00Z"
    }
  ],
  "timestamp": "2026-03-10T13:35:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/price-records/commodity/1`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

---

### 24. Get Price Records by Market

**Purpose**: Retrieve all price records for a specific market.

**Endpoint**: `GET /api/v1/price-records/market/{marketId}`

**URL**: `{{base_url}}/api/v1/price-records/market/1`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `marketId`: Market ID (e.g., 1)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "id": 1,
      "commodityId": 1,
      "commodityName": "Rice",
      "marketId": 1,
      "marketName": "Makola Market",
      "price": 45.50,
      "recordedDate": "2026-03-09",
      "source": "Market Survey",
      "createdAt": "2026-03-09T14:00:00Z"
    },
    {
      "id": 2,
      "commodityId": 2,
      "commodityName": "Tomatoes",
      "marketId": 1,
      "marketName": "Makola Market",
      "price": 12.00,
      "recordedDate": "2026-03-09",
      "source": "Market Survey",
      "createdAt": "2026-03-09T14:00:00Z"
    }
  ],
  "timestamp": "2026-03-10T13:40:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/price-records/market/1`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

---

### 25. Create New Price Record (Admin Only)

**Purpose**: Add a new price record to the system.

**Endpoint**: `POST /api/v1/price-records`

**URL**: `{{base_url}}/api/v1/price-records`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Request Body**:
```json
{
  "commodityId": 1,
  "marketId": 1,
  "price": 46.75,
  "recordedDate": "2026-03-10",
  "source": "Daily Market Survey"
}
```

**Alternative Examples**:
```json
{
  "commodityId": 2,
  "marketId": 2,
  "price": 15.50,
  "recordedDate": "2026-03-10",
  "source": "Vendor Report"
}
```

```json
{
  "commodityId": 3,
  "marketId": 1,
  "price": 28.00,
  "recordedDate": "2026-03-10",
  "source": null
}
```

**Example Response** (201 Created):
```json
{
  "status": "success",
  "message": "Price record created successfully",
  "data": {
    "id": 51,
    "commodityId": 1,
    "commodityName": "Rice",
    "marketId": 1,
    "marketName": "Makola Market",
    "price": 46.75,
    "recordedDate": "2026-03-10",
    "source": "Daily Market Survey",
    "createdAt": "2026-03-10T13:45:00Z"
  },
  "timestamp": "2026-03-10T13:45:00Z"
}
```

**Postman Testing Steps**:
1. Create a new POST request
2. Enter the URL: `{{base_url}}/api/v1/price-records`
3. Go to Headers tab, add:
   - `Content-Type: application/json`
   - `Authorization: Bearer {{jwt_token}}`
4. Go to Body tab, select "raw" and "JSON"
5. Paste the request body
6. Click "Send"

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `400 Bad Request`: Validation errors (missing fields, negative price, future date)
- `404 Not Found`: Commodity or Market with specified ID does not exist

---

### 26. Update Price Record (Admin Only)

**Purpose**: Update an existing price record.

**Endpoint**: `PUT /api/v1/price-records/{id}`

**URL**: `{{base_url}}/api/v1/price-records/51`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: Price record ID to update (e.g., 51)

**Request Body**:
```json
{
  "commodityId": 1,
  "marketId": 1,
  "price": 47.00,
  "recordedDate": "2026-03-10",
  "source": "Corrected Market Survey"
}
```

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Price record updated successfully",
  "data": {
    "id": 51,
    "commodityId": 1,
    "commodityName": "Rice",
    "marketId": 1,
    "marketName": "Makola Market",
    "price": 47.00,
    "recordedDate": "2026-03-10",
    "source": "Corrected Market Survey",
    "createdAt": "2026-03-10T13:45:00Z"
  },
  "timestamp": "2026-03-10T13:50:00Z"
}
```

**Postman Testing Steps**:
1. Create a new PUT request
2. Enter the URL: `{{base_url}}/api/v1/price-records/51`
3. Go to Headers tab, add:
   - `Content-Type: application/json`
   - `Authorization: Bearer {{jwt_token}}`
4. Go to Body tab, select "raw" and "JSON"
5. Paste the request body
6. Click "Send"

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `404 Not Found`: Price record with specified ID does not exist
- `400 Bad Request`: Validation errors

---

### 27. Delete Price Record (Admin Only)

**Purpose**: Remove a price record from the system.

**Endpoint**: `DELETE /api/v1/price-records/{id}`

**URL**: `{{base_url}}/api/v1/price-records/51`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `id`: Price record ID to delete (e.g., 51)

**Example Response** (204 No Content):
```
(Empty response body)
```

**Postman Testing Steps**:
1. Create a new DELETE request
2. Enter the URL: `{{base_url}}/api/v1/price-records/51`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"
5. Verify status code is 204

**Error Responses**:
- `403 Forbidden`: Not authorized (requires ADMIN role)
- `404 Not Found`: Price record with specified ID does not exist

---

## Analytics Endpoints

### 28. Get Monthly Price Trend

**Purpose**: Retrieve monthly average price trends for a specific commodity.

**Endpoint**: `GET /api/v1/analytics/trends/{commodityId}`

**URL**: `{{base_url}}/api/v1/analytics/trends/1?months=12`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `commodityId`: Commodity ID (e.g., 1)

**Query Parameters**:
- `months`: Number of months to analyze (default: 12)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "commodityId": 1,
      "commodityName": "Rice",
      "month": "2025-04",
      "averagePrice": 42.50
    },
    {
      "commodityId": 1,
      "commodityName": "Rice",
      "month": "2025-05",
      "averagePrice": 43.75
    },
    {
      "commodityId": 1,
      "commodityName": "Rice",
      "month": "2025-06",
      "averagePrice": 44.20
    },
    {
      "commodityId": 1,
      "commodityName": "Rice",
      "month": "2026-03",
      "averagePrice": 46.15
    }
  ],
  "timestamp": "2026-03-10T14:00:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/analytics/trends/1`
3. Go to Params tab, add query parameter:
   - `months`: 12
4. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
5. Click "Send"

**Error Responses**:
- `404 Not Found`: Commodity with specified ID does not exist

---

### 29. Get City Price Comparison

**Purpose**: Compare average prices of a commodity across different cities.

**Endpoint**: `GET /api/v1/analytics/city-comparison/{commodityId}`

**URL**: `{{base_url}}/api/v1/analytics/city-comparison/1`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `commodityId`: Commodity ID (e.g., 1)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "cityName": "Kumasi",
      "commodityName": "Rice",
      "averagePrice": 47.80
    },
    {
      "cityName": "Accra",
      "commodityName": "Rice",
      "averagePrice": 45.25
    },
    {
      "cityName": "Tamale",
      "commodityName": "Rice",
      "averagePrice": 41.50
    }
  ],
  "timestamp": "2026-03-10T14:05:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/analytics/city-comparison/1`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

**Error Responses**:
- `404 Not Found`: Commodity with specified ID does not exist

---

### 30. Get Price Volatility

**Purpose**: Calculate price volatility (standard deviation) for all commodities.

**Endpoint**: `GET /api/v1/analytics/volatility`

**URL**: `{{base_url}}/api/v1/analytics/volatility`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": [
    {
      "commodityId": 2,
      "commodityName": "Tomatoes",
      "standardDeviation": 8.45,
      "interpretation": "MEDIUM"
    },
    {
      "commodityId": 3,
      "commodityName": "Cooking Oil",
      "standardDeviation": 3.20,
      "interpretation": "LOW"
    },
    {
      "commodityId": 1,
      "commodityName": "Rice",
      "standardDeviation": 2.15,
      "interpretation": "LOW"
    }
  ],
  "timestamp": "2026-03-10T14:10:00Z"
}
```

**Interpretation Values**:
- `LOW`: Standard deviation < 5
- `MEDIUM`: Standard deviation between 5 and 20
- `HIGH`: Standard deviation > 20

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/analytics/volatility`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

---

### 31. Get Inflation Trend

**Purpose**: Analyze inflation trend for a commodity (current month vs. last month).

**Endpoint**: `GET /api/v1/analytics/inflation/{commodityId}`

**URL**: `{{base_url}}/api/v1/analytics/inflation/1`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `commodityId`: Commodity ID (e.g., 1)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": {
    "commodityId": 1,
    "commodityName": "Rice",
    "currentMonthAverage": 46.15,
    "lastMonthAverage": 44.80,
    "percentageChange": 3.01,
    "direction": "UP"
  },
  "timestamp": "2026-03-10T14:15:00Z"
}
```

**Direction Values**:
- `UP`: Price increased by more than 1%
- `DOWN`: Price decreased by more than 1%
- `STABLE`: Price change is between -1% and +1%

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/analytics/inflation/1`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

**Error Responses**:
- `404 Not Found`: Commodity with specified ID does not exist
- `204 No Content`: Insufficient data (less than 2 months of data available)

---

### 32. Get Moving Average Forecast

**Purpose**: Get price forecast for next month based on 3-month moving average.

**Endpoint**: `GET /api/v1/analytics/forecast/{commodityId}`

**URL**: `{{base_url}}/api/v1/analytics/forecast/1`

**Headers**:
```
Authorization: Bearer {{jwt_token}}
```

**Path Variables**:
- `commodityId`: Commodity ID (e.g., 1)

**Example Response** (200 OK):
```json
{
  "status": "success",
  "message": "Request successful",
  "data": {
    "commodityId": 1,
    "commodityName": "Rice",
    "forecastMonth": "2026-04",
    "forecastedPrice": 45.72,
    "basedOnMonths": 3
  },
  "timestamp": "2026-03-10T14:20:00Z"
}
```

**Postman Testing Steps**:
1. Create a new GET request
2. Enter the URL: `{{base_url}}/api/v1/analytics/forecast/1`
3. Go to Headers tab, add `Authorization: Bearer {{jwt_token}}`
4. Click "Send"

**Error Responses**:
- `404 Not Found`: Commodity with specified ID does not exist
- `204 No Content`: Insufficient data (less than 3 months of data available)

---

## Complete Testing Workflow

### Recommended Testing Order

1. **Authentication**
   - Login with admin credentials (Endpoint #1)
   - Save JWT token for subsequent requests

2. **Setup Master Data**
   - Create cities (Endpoint #6)
   - Create markets (Endpoint #12)
   - Create commodities (Endpoint #18)

3. **Record Price Data**
   - Create price records (Endpoint #25)
   - Create multiple records across different dates, markets, and commodities

4. **Query Data**
   - Get all cities (Endpoint #3)
   - Get markets by city (Endpoint #11)
   - Get commodities by category (Endpoint #17)
   - Get price records by commodity (Endpoint #23)
   - Get price records by market (Endpoint #24)

5. **Analytics**
   - Get monthly trends (Endpoint #28)
   - Compare prices across cities (Endpoint #29)
   - Check price volatility (Endpoint #30)
   - Analyze inflation trends (Endpoint #31)
   - Get price forecasts (Endpoint #32)

6. **User Management**
   - Register new users (Endpoint #2)
   - Test with VIEWER role (limited access)

7. **Update & Delete Operations**
   - Update records (Endpoints #7, #13, #19, #26)
   - Delete records (Endpoints #8, #14, #20, #27)

---

## Postman Collection Setup

### Creating Environment Variables

1. Click on "Environments" in Postman
2. Create a new environment called "Commodity Monitor - Local"
3. Add these variables:
   - `base_url`: `http://localhost:8080`
   - `jwt_token`: (leave empty, will be auto-populated)

### Auto-Save JWT Token Script

Add this to the "Tests" tab of your login request:

```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("jwt_token", jsonData.data.token);
    console.log("JWT Token saved: " + jsonData.data.token);
}
```

### Global Pre-request Script (Optional)

For debugging, add this to Collection > Pre-request Scripts:

```javascript
console.log("Request URL: " + pm.request.url);
console.log("Request Method: " + pm.request.method);
console.log("JWT Token: " + pm.environment.get("jwt_token"));
```

### Global Test Script (Optional)

For automatic validation, add this to Collection > Tests:

```javascript
pm.test("Status code is successful", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201, 204]);
});

pm.test("Response time is less than 2000ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

if (pm.response.code !== 204) {
    pm.test("Response has correct structure", function () {
        var jsonData = pm.response.json();
        pm.expect(jsonData).to.have.property('status');
        pm.expect(jsonData).to.have.property('timestamp');
    });
}
```

---

## Common Error Responses

### 400 Bad Request
```json
{
  "status": "error",
  "message": "Validation failed",
  "errors": {
    "name": "City name is required",
    "region": "Region name is required"
  },
  "timestamp": "2026-03-10T14:25:00Z"
}
```

### 401 Unauthorized
```json
{
  "status": "error",
  "message": "Invalid credentials",
  "timestamp": "2026-03-10T14:25:00Z"
}
```

### 403 Forbidden
```json
{
  "status": "error",
  "message": "Access denied. Insufficient permissions.",
  "timestamp": "2026-03-10T14:25:00Z"
}
```

### 404 Not Found
```json
{
  "status": "error",
  "message": "City not found with id: 999",
  "timestamp": "2026-03-10T14:25:00Z"
}
```

### 409 Conflict
```json
{
  "status": "error",
  "message": "City with name 'Accra' already exists",
  "timestamp": "2026-03-10T14:25:00Z"
}
```

---

## Tips for Effective Testing

1. **Use Collections**: Organize all requests into a Postman collection for easy management

2. **Test Sequences**: Create test sequences using Postman's Collection Runner to test complete workflows

3. **Data-Driven Testing**: Use CSV files with Collection Runner to test multiple data scenarios

4. **Environment Switching**: Create separate environments for dev, staging, and production

5. **Documentation**: Use Postman's documentation feature to generate API docs from your collection

6. **Monitoring**: Set up Postman monitors to run tests periodically

7. **Version Control**: Export and commit your Postman collection to version control

8. **Mock Servers**: Use Postman mock servers for frontend development before backend is ready

---

## Sample Test Data

### Cities
```
Accra - Greater Accra
Kumasi - Ashanti
Tamale - Northern
Takoradi - Western
Cape Coast - Central
```

### Markets
```
Makola Market - Accra
Kaneshie Market - Accra
Kejetia Market - Kumasi
Central Market - Tamale
Market Circle - Takoradi
```

### Commodities
```
Rice - Grains - kg
Maize - Grains - kg
Tomatoes - Vegetables - kg
Onions - Vegetables - kg
Cooking Oil - Oils - liter
Palm Oil - Oils - liter
Yam - Tubers - tuber
Cassava - Tubers - kg
```

---

## Troubleshooting

### Issue: 401 Unauthorized on all requests
**Solution**: Ensure you've logged in and the JWT token is saved in environment variables

### Issue: 403 Forbidden on create/update/delete
**Solution**: Verify you're logged in as ADMIN role, not VIEWER

### Issue: 404 Not Found on analytics endpoints
**Solution**: Ensure the commodity exists and has sufficient price data

### Issue: 204 No Content on forecast/inflation
**Solution**: Add more historical price records (need at least 3 months of data)

### Issue: Connection refused
**Solution**: Verify the Spring Boot application is running on port 8080

---

## Next Steps

1. Import this guide into Postman as documentation
2. Create a collection with all 32 endpoints
3. Set up environment variables
4. Run the complete testing workflow
5. Export the collection for team sharing
6. Set up automated tests using Collection Runner

---

**Document Version**: 1.0  
**Last Updated**: March 10, 2026  
**API Base URL**: http://localhost:8080  
**API Version**: v1
