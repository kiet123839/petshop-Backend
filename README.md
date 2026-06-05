# petshop-backend

## Run In IntelliJ IDEA

1. Open folder `petshop-backend-develop` as a Maven project.
2. Wait for IntelliJ to finish Maven import.
3. Make sure the project SDK is Java 17 or newer.
4. If Lombok fields still show red in the editor, install the Lombok plugin in IntelliJ and enable annotation processing:
   `Settings > Build, Execution, Deployment > Compiler > Annotation Processors > Enable annotation processing`
5. Start the app by running `PetshopAuthApplication`.

## Run In Terminal

Use the Maven wrapper included in the project:

```powershell
.\mvnw.cmd spring-boot:run
```

To test email locally:

1. Copy `src/main/resources/application-local.yml.example` to `src/main/resources/application-local.yml`
2. Fill in your Gmail SMTP account and app password
3. Run with the `local` profile:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Test email API:

```http
POST /api/notifications/email/test
Content-Type: application/json

{
  "to": "your-email@example.com",
  "subject": "Test SMTP",
  "message": "Hello from PetShop backend"
}
```

## Database

Current backend configuration expects:

- SQL Server running on `localhost:1433`
- Database name: `PETSHOP_DB`
- Username: `sa`
- Password: `123456`

Connection config is in `src/main/resources/application.yml`.
You can override it without editing the file:

```powershell
$env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=PETSHOP_DB;encrypt=false;trustServerCertificate=true"
$env:DB_USERNAME="sa"
$env:DB_PASSWORD="123456"
$env:SERVER_PORT="8080"
.\mvnw.cmd spring-boot:run
```

If port `8080` is already in use, stop the old backend process or run on another port:

```powershell
$env:SERVER_PORT="8081"
.\mvnw.cmd spring-boot:run
```

## VNPAY

Set these environment variables before starting the backend:

```powershell
$env:VNPAY_TMN_CODE="your_sandbox_tmn_code"
$env:VNPAY_HASH_SECRET="your_sandbox_hash_secret"
$env:VNPAY_RETURN_URL="http://localhost:8080/api/payments/vnpay/return"
```

The default payment gateway URL is the VNPAY sandbox URL:
`https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`.
