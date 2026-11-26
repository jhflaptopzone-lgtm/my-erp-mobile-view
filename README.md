# JHF Laptop Zone — Mobile ERP (Android)

Native Kotlin & Jetpack Compose Android application for **JHF Laptop Zone**, connected live to the web portal (`https://jhflaptop.cloud.run/`). Any inventory edit, POS sale, EMI installment collection, supplier payment, or staff change performed on mobile immediately updates the live web server and cloud mirror.

## Key Capabilities
- **Live Web Sync**: Connects directly to `https://jhflaptop.cloud.run` (configurable in Login & Settings) via JWT-authenticated REST APIs.
- **Dashboard & Analytics**: Real-time sales totals, net profit (Admin), EMI/Due outstanding, low-stock alerts, Monthly Sales Trend chart, Payment Distribution chart, and Supplier Due summary.
- **Inventory Management**: 85+ preset laptop specs autocomplete, CameraX + ML Kit barcode/serial scanner, stock PDF reports, and full CRUD synced to web.
- **Sales POS & Invoices**: Cash, Bank Transfer, EMI, and Due sales workflows with instant QR share links and A4 PDF invoice generation (`PdfDocument` + `FileProvider`).
- **EMI & Due Tracking**: Installment timeline, payment recording (`Cash`, `Bank`, `Bkash`, `Nagad`, `Rocket`, `POS`), overdue highlighting, CSV export, and Due Report PDFs.
- **Supplier Ledger & Accounting**: Vendor list, purchase & payment ledgers, supplier payments, and filtered statement PDFs.
- **Staff & Cloud Tools**: Role-based access (`Admin`, `Manager`, `Staff`), Google Cloud Sign-In (`CredentialManager`) with Firestore workspace & sync audit logs, JSON backup export/restore via Storage Access Framework, and Public Customer Invoice lookup.
