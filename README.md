# Micro-Commerce Application

This project is a microservice-based e-commerce application built with Java and Spring Boot.

## Architecture

The application is composed of three core microservices:

- **product-service**: Manages the product catalog.
- **customer-service**: Manages customer information.
- **order-service**: Manages customer orders.

Each service is autonomous, has its own database, and exposes a REST API. The services communicate with each other synchronously for data validation.

## Prerequisites

Before you begin, ensure you have met the following requirements:
- Java 17
- Maven
- Docker
- Docker Compose

## Running the Application

The easiest way to run the application is by using Docker Compose. This will build and run all services in a containerized environment.

1.  **Build the Docker images:**
    From the root directory of the project, run:
    ```bash
    docker-compose build
    ```

2.  **Start the services:**
    ```bash
    docker-compose up -d
    ```
    This command starts all three microservices in detached mode.
    - `product-service` will be available on port `8081`.
    - `customer-service` will be available on port `8082`.
    - `order-service` will be available on port `8083`.

## Testing the Application

You can use `curl` to interact with the services. The APIs are secured with HTTP Basic Authentication. The credentials are `admin:password`.

### 1. Create a Product
```bash
curl -u admin:password -X POST http://localhost:8081/api/products -H "Content-Type: application/json" -d '{"name":"Laptop","description":"A powerful laptop","price":1200.50}'
```

### 2. Create a Customer
```bash
curl -u admin:password -X POST http://localhost:8082/api/customers -H "Content-Type: application/json" -d '{"firstName":"John","lastName":"Doe","email":"john.doe@example.com"}'
```

### 3. Create an Order
Make sure to use the `id` of the product and customer you created in the previous steps (in this example, both are `1`).
```bash
curl -u admin:password -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d '{"customerId":1,"orderDate":"2025-07-02","status":"PENDING","orderItems":[{"productId":1,"quantity":1,"price":1200.50}]}'
```

### 4. Get the Order
```bash
curl http://localhost:8083/api/orders/1
```

## Stopping the Application

To stop all running services, use the following command:
```bash
docker-compose down
```
This will stop and remove the containers and the network created by `docker-compose up`.
