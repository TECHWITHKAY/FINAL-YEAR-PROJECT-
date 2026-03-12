# CommodityGH — Backend API

A robust Spring Boot 3 REST API for the Ghana Commodity Price Monitoring and Analytics System. This backend provides real-time tracking, historical analysis, and intelligent forecasting for commodity prices across major Ghanaian markets.

## 🚀 Key Features

- **Price Monitoring**: Real-time tracking of prices for Maize, Rice, Tomato, Yam, Plantain, and Groundnuts.
- **Advanced Analytics**:
  - **Volatility Ranking**: Identifying the most stable and volatile commodities.
  - **Inflation Monitoring**: Tracking price changes relative to historical averages.
  - **Forecasting**: Moving average predictions for future price trends.
- **Seasonal Analysis**: Outlooks and historical patterns for better market timing.
- **Market Health Scoring**: A grading system (A–F) for markets based on data availability and price stability.
- **Data Integrity**: Automated Data Quality Reports and outlier detection.
- **Export System**: Multi-format (CSV/Excel) export functionality for historical records.
- **Security**: JWT-based Authentication with Role-Based Access Control (Admin, Field Agent, Analyst).
- **Automated Seeding**: Intelligent data seeder for historical analysis and testing.

## 🛠 Tech Stack

- **Core**: Java 21, Spring Boot 3.5.11
- **Persistence**: Spring Data JPA, Hibernate, PostgreSQL
- **Security**: Spring Security 6, JJWT (JSON Web Token)
- **Migrations**: Flyway
- **Documentation**: Springdoc-OpenAPI (Swagger UI)
- **Utilities**: Lombok, Apache POI (Excel), Spring Mail, Spring Cache
- **Testing**: JUnit 5, Mockito, Testcontainers

## ⚙️ Getting Started

### Prerequisites

- **JDK 21** or higher
- **PostgreSQL** instance
- **Maven** 3.9+

### Configuration

Update `src/main/resources/application.yaml` with your local environment variables or set them in your environment:

| Variable | Description | Default |
|---|---|---|
| `DB_PASSWORD` | PostgreSQL password | (your password) |
| `MAIL_ENABLED` | Enable email notifications | `false` |
| `MAIL_USERNAME` | SMTP User | (your email) |
| `MAIL_PASSWORD` | SMTP App Password | (your app password) |
| `FRONTEND_URL` | Base URL of the React app | `http://localhost:5173` |

### Running the Application

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd commodity-monitor
   ```

2. **Run with Maven**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Access API Documentation**
   Once running, visit: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## 📁 Project Structure

- `controller`: REST API endpoints for analytics, auth, commodities, markets, and users.
- `service`: Core business logic, computations, and external service integrations.
- `entity/dto`: Data models and Data Transfer Objects for API requests/responses.
- `security`: JWT configuration, filters, and authentication providers.
- `scheduler`: Background tasks for health score computation and pattern updates.
- `util/seeder`: Helper classes and the automated historical data generator.

## 🧪 Testing

Run the test suite using Maven:

```bash
./mvnw test
```

---
© 2026 CommodityGH. Built for Ghana's agricultural data transparency.