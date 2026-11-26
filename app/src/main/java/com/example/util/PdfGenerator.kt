package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.DueReportItem
import com.example.data.model.InstallmentRecord
import com.example.data.model.InventoryItem
import com.example.data.model.SaleRecord
import com.example.data.model.SupplierItem
import com.example.data.model.SupplierReportEntry
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private fun reportDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun openPdfOrShare(context: Context, file: File, shareOnly: Boolean = false) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        if (!shareOnly) {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = runCatching {
                context.startActivity(Intent.createChooser(viewIntent, "Open PDF Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrDefault(false)
            if (chooser) return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun createCsvFile(context: Context, fileName: String, csvContent: String): File {
        val file = File(reportDir(context), fileName)
        file.writeText(csvContent, Charsets.UTF_8)
        return file
    }

    fun createJsonFile(context: Context, fileName: String, jsonContent: String): File {
        val file = File(reportDir(context), fileName)
        file.writeText(jsonContent, Charsets.UTF_8)
        return file
    }

    fun generateInvoicePdf(
        context: Context,
        sale: SaleRecord,
        installments: List<InstallmentRecord> = emptyList()
    ): File {
        // A4 Portrait (595 x 842 pt) matching server.ts buildInvoicePDF
        val pageWidth = 595
        val pageHeight = 842
        val margin = 30f
        val contentWidth = pageWidth - margin * 2

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdf.startPage(pageInfo)
        val c = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        val bodyCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
        }
        val regularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        var y = 48f
        c.drawText("JHF Laptop Zone", pageWidth / 2f, y, titlePaint)
        y += 14f
        listOf(
            "Best Laptop Buy, Sell & Accessories Importer, Traders & Suppliers",
            "Branch Office : Baroipara Bazar, Chandra, Gazipur",
            "Main Office : Sonali Trade, Shop No : 1001 & 1040",
            "ECS Computer City Centre, Multiplan, New Elephant Road, Dhaka",
            "Mobile : 01935-693071 , 01731-693071",
            "Email : jhflaptopzone@gmail.com",
            "Facebook : www.facebook.com/jhflaptopzone",
            "Web : www.jhflaptopzone.com"
        ).forEach { line ->
            c.drawText(line, pageWidth / 2f, y, bodyCenter)
            y += 11.5f
        }

        y += 4f
        c.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 22f

        titlePaint.textSize = 17f
        c.drawText("SALES INVOICE", pageWidth / 2f, y, titlePaint)
        y += 22f

        val rightPaint = Paint(regularPaint).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("Customer : ${sale.customerName}", margin, y, regularPaint)
        c.drawText("Invoice No : ${sale.invoiceNumber}", pageWidth - margin, y, rightPaint)
        y += 14f
        c.drawText("Mobile : ${sale.customerPhone}", margin, y, regularPaint)
        c.drawText("Date : ${Formatters.formatDateBD(sale.saleDate)}", pageWidth - margin, y, rightPaint)
        y += 14f
        c.drawText("Address : ${sale.customerAddress?.ifBlank { "N/A" } ?: "N/A"}", margin, y, regularPaint)
        c.drawText("Sales By : ${sale.staffName ?: "Jahid Hasan"}", pageWidth - margin, y, rightPaint)
        y += 20f

        // Table Header
        val colWidths = floatArrayOf(30f, 250f, 50f, 90f, 115f)
        val colX = FloatArray(colWidths.size + 1)
        colX[0] = margin
        for (i in colWidths.indices) colX[i + 1] = colX[i] + colWidths[i]

        val headerH = 22f
        c.drawRect(margin, y, margin + contentWidth, y + headerH, linePaint)
        val centerBold = Paint(boldPaint).apply { textAlign = Paint.Align.CENTER }
        listOf("SL", "Item Name", "Qty", "Unit Price", "Amount").forEachIndexed { idx, h ->
            c.drawText(h, colX[idx] + colWidths[idx] / 2f, y + 15f, centerBold)
        }

        val itemTop = y + headerH
        val modelLine = sale.model ?: "Laptop"
        val configLine = sale.config ?: ""
        val serialLine = "Serial No : ${sale.serialNumber ?: "Not Applicable"}"
        val rowH = 56f
        c.drawRect(margin, itemTop, margin + contentWidth, itemTop + rowH, linePaint)
        for (i in 1 until colWidths.size) {
            c.drawLine(colX[i], y, colX[i], itemTop + rowH, linePaint)
        }

        val centerReg = Paint(regularPaint).apply { textAlign = Paint.Align.CENTER }
        c.drawText("1", colX[0] + colWidths[0] / 2f, itemTop + 20f, centerReg)
        c.drawText(modelLine.take(40), colX[1] + 6f, itemTop + 16f, regularPaint)
        c.drawText(configLine.take(44), colX[1] + 6f, itemTop + 31f, Paint(regularPaint).apply { textSize = 8.5f })
        c.drawText(serialLine.take(40), colX[1] + 6f, itemTop + 46f, Paint(regularPaint).apply { textSize = 9f })
        c.drawText("1", colX[2] + colWidths[2] / 2f, itemTop + 20f, centerReg)
        c.drawText(Formatters.formatNumber(sale.sellPrice), colX[3] + colWidths[3] / 2f, itemTop + 20f, centerReg)
        c.drawText(Formatters.formatNumber(sale.sellPrice), colX[4] + colWidths[4] - 6f, itemTop + 20f, rightPaint)

        y = itemTop + rowH + 10f

        // Total Box
        val boxW = 145f
        val boxX = pageWidth - margin - boxW
        val boxH = 22f
        c.drawRect(boxX, y, boxX + boxW, y + boxH, linePaint)
        c.drawLine(boxX + 52f, y, boxX + 52f, y + boxH, linePaint)
        c.drawText("Total", boxX + 26f, y + 15f, centerBold)
        c.drawText("${Formatters.formatNumber(sale.sellPrice)} TK", boxX + boxW - 6f, y + 15f, Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT })

        y += boxH + 16f
        c.drawText("In Words : ${Formatters.numberToWords(sale.sellPrice)} Taka Only", margin, y, boldPaint)
        y += 20f

        c.drawText("Warranty Policy", margin, y, Paint(boldPaint).apply { textSize = 11f })
        y += 14f
        c.drawText("  \u2022 Full Laptop & Spare Parts : 1 Month", margin, y, Paint(regularPaint).apply { textSize = 9.5f })
        y += 13f
        c.drawText("  \u2022 Service : 5 Years", margin, y, Paint(regularPaint).apply { textSize = 9.5f })
        y += 20f

        if (installments.isNotEmpty()) {
            c.drawText("Payment / Installment Details", margin, y, Paint(boldPaint).apply { textSize = 11f })
            y += 8f
            val iWidths = floatArrayOf(35f, 105f, 100f, 95f)
            val iTotal = iWidths.sum()
            val iX = FloatArray(iWidths.size + 1)
            iX[0] = margin
            for (i in iWidths.indices) iX[i + 1] = iX[i] + iWidths[i]

            c.drawRect(margin, y, margin + iTotal, y + 16f, linePaint)
            listOf("No", "Due Date", "Amount", "Status").forEachIndexed { idx, h ->
                c.drawText(h, iX[idx] + iWidths[idx] / 2f, y + 11.5f, Paint(centerBold).apply { textSize = 8.5f })
            }
            y += 16f
            installments.take(12).forEachIndexed { idx, inst ->
                if (y < pageHeight - 135f) {
                    c.drawRect(margin, y, margin + iTotal, y + 15f, linePaint)
                    val sPaint = Paint(centerReg).apply { textSize = 8.5f }
                    c.drawText((idx + 1).toString(), iX[0] + iWidths[0] / 2f, y + 11f, sPaint)
                    c.drawText(Formatters.formatDateBD(inst.dueDate), iX[1] + iWidths[1] / 2f, y + 11f, sPaint)
                    c.drawText(Formatters.formatNumber(inst.amount), iX[2] + iWidths[2] / 2f, y + 11f, sPaint)
                    c.drawText(inst.status, iX[3] + iWidths[3] / 2f, y + 11f, sPaint)
                    y += 15f
                }
            }
        }

        // Signatures & Footer
        val footerY = maxOf(y + 30f, pageHeight - 110f)
        c.drawLine(margin, footerY, margin + 150f, footerY, linePaint)
        c.drawText("Customer Signature", margin + 75f, footerY + 14f, centerReg)

        c.drawLine(pageWidth - margin - 150f, footerY, pageWidth - margin, footerY, linePaint)
        c.drawText("Authorized Signature", pageWidth - margin - 75f, footerY + 14f, centerReg)

        val grayItalic = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        c.drawText(
            "This is a computer-generated document and does not require a physical signature.",
            pageWidth / 2f,
            footerY + 32f,
            grayItalic
        )
        c.drawText(
            "Thank You For Your Purchase",
            pageWidth / 2f,
            footerY + 49f,
            Paint(boldPaint).apply { textSize = 12f; textAlign = Paint.Align.CENTER }
        )
        c.drawText(
            "Software Developed By Jahid Hasan | JHF Laptop Zone",
            pageWidth / 2f,
            footerY + 66f,
            Paint(regularPaint).apply {
                color = Color.parseColor("#4B5563")
                textSize = 8f
                textAlign = Paint.Align.CENTER
            }
        )

        pdf.finishPage(page)
        val outFile = File(reportDir(context), "Invoice_${sale.invoiceNumber}.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        return outFile
    }

    fun generateInventoryReportPdf(
        context: Context,
        items: List<InventoryItem>,
        isAdmin: Boolean
    ): File {
        val available = items.filter { it.status.equals("Available", ignoreCase = true) }
        val pageWidth = 842
        val pageHeight = 595
        val margin = 30f
        val pdf = PdfDocument()

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var c = page.canvas

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
        }
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val fillHeaderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.FILL
        }

        var y = 44f
        c.drawText("JHF Laptop Zone", pageWidth / 2f, y, headerPaint)
        y += 14f
        c.drawText("Inventory Stock Report | Generated on: ${Formatters.nowDhakaDateTime()}", pageWidth / 2f, y, subPaint)
        y += 12f
        c.drawLine(margin, y, pageWidth - margin, y, borderPaint)
        y += 16f

        val totalBuy = available.sumOf { it.buyPrice }
        val totalSell = available.sumOf { it.sellPrice }
        if (isAdmin) {
            c.drawText(
                "Total Available: ${available.size} units | Total Buy: ${Formatters.formatTaka(totalBuy)} | Total Sell: ${Formatters.formatTaka(totalSell)} | Est. Profit: ${Formatters.formatTaka(totalSell - totalBuy)}",
                margin,
                y,
                boldPaint
            )
            y += 18f
        }

        val colWidths = if (isAdmin) {
            floatArrayOf(35f, 230f, 115f, 80f, 80f, 80f, 92f, 70f)
        } else {
            floatArrayOf(40f, 330f, 150f, 110f, 152f)
        }
        val headers = if (isAdmin) {
            listOf("SL", "Model & Config", "Serial", "Buy", "Sell", "Profit", "Supplier", "Date")
        } else {
            listOf("SL", "Model & Config", "Serial", "Sell Price", "Supplier")
        }

        fun drawHeader(atY: Float): Float {
            val totalW = colWidths.sum()
            c.drawRect(margin, atY, margin + totalW, atY + 20f, fillHeaderPaint)
            c.drawRect(margin, atY, margin + totalW, atY + 20f, borderPaint)
            var x = margin
            headers.forEachIndexed { idx, h ->
                c.drawText(h, x + 4f, atY + 13.5f, boldPaint)
                x += colWidths[idx]
                c.drawLine(x, atY, x, atY + 20f, borderPaint)
            }
            return atY + 20f
        }

        y = drawHeader(y)
        available.forEachIndexed { idx, item ->
            if (y > pageHeight - 50f) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                c = page.canvas
                y = drawHeader(36f)
            }
            val rowH = 22f
            val totalW = colWidths.sum()
            c.drawRect(margin, y, margin + totalW, y + rowH, borderPaint)
            var x = margin
            val cells = if (isAdmin) {
                listOf(
                    (idx + 1).toString(),
                    "${item.model} (${item.config})".take(42),
                    item.serialNumber.take(20),
                    Formatters.formatNumber(item.buyPrice),
                    Formatters.formatNumber(item.sellPrice),
                    Formatters.formatNumber(item.sellPrice - item.buyPrice),
                    (item.supplierName ?: "N/A").take(15),
                    Formatters.formatDateBD(item.purchaseDate).take(12)
                )
            } else {
                listOf(
                    (idx + 1).toString(),
                    "${item.model} (${item.config})".take(55),
                    item.serialNumber.take(25),
                    Formatters.formatNumber(item.sellPrice),
                    (item.supplierName ?: "N/A").take(25)
                )
            }
            cells.forEachIndexed { cIdx, v ->
                c.drawText(v, x + 4f, y + 14.5f, textPaint)
                x += colWidths[cIdx]
                c.drawLine(x, y, x, y + rowH, borderPaint)
            }
            y += rowH
        }

        pdf.finishPage(page)
        val outFile = File(reportDir(context), "Inventory_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        return outFile
    }

    fun generateDueReportPdf(context: Context, items: List<DueReportItem>): File {
        val pageWidth = 842
        val pageHeight = 595
        val margin = 30f
        val pdf = PdfDocument()

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var c = page.canvas

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
        }
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val neonFill = Paint().apply {
            color = Color.parseColor("#39FF14")
            style = Paint.Style.FILL
        }

        var y = 42f
        c.drawText("JHF Laptop Zone - DUE REPORT", pageWidth / 2f, y, headerPaint)
        y += 15f
        c.drawText("Generated on: ${Formatters.nowDhakaDateTime()}", pageWidth / 2f, y, Paint(textPaint).apply { textAlign = Paint.Align.CENTER })
        y += 18f

        val colWidths = floatArrayOf(125f, 95f, 80f, 75f, 75f, 75f, 80f, 102f, 75f)
        val headers = listOf("Model", "Serial", "Sale Date", "Sell Price", "Paid", "Remaining", "Final Due", "Customer", "Phone")

        fun drawHeader(atY: Float): Float {
            val totalW = colWidths.sum()
            c.drawRect(margin, atY, margin + totalW, atY + 20f, neonFill)
            c.drawRect(margin, atY, margin + totalW, atY + 20f, borderPaint)
            var x = margin
            headers.forEachIndexed { idx, h ->
                c.drawText(h, x + 4f, atY + 13.5f, boldPaint)
                x += colWidths[idx]
                c.drawLine(x, atY, x, atY + 20f, borderPaint)
            }
            return atY + 20f
        }

        y = drawHeader(y)
        var totalOutstanding = 0.0
        items.forEach { item ->
            val rem = item.sellPrice - item.totalPaid
            totalOutstanding += rem
            if (y > pageHeight - 60f) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                c = page.canvas
                y = drawHeader(36f)
            }
            val rowH = 20f
            val totalW = colWidths.sum()
            c.drawRect(margin, y, margin + totalW, y + rowH, borderPaint)
            var x = margin
            listOf(
                item.model.take(22),
                item.serialNumber.take(16),
                Formatters.formatDateBD(item.saleDate).take(12),
                Formatters.formatNumber(item.sellPrice),
                Formatters.formatNumber(item.totalPaid),
                Formatters.formatNumber(rem),
                Formatters.formatDateBD(item.finalDueDate).take(12),
                item.customerName.take(18),
                item.customerPhone.take(14)
            ).forEachIndexed { cIdx, v ->
                c.drawText(v, x + 4f, y + 13.5f, textPaint)
                x += colWidths[cIdx]
                c.drawLine(x, y, x, y + rowH, borderPaint)
            }
            y += rowH
        }

        y += 18f
        c.drawText("Total Outstanding Due: ${Formatters.formatTaka(totalOutstanding)}", margin, y, Paint(boldPaint).apply { textSize = 11f })
        c.drawText("Software Developed By Jahid Hasan | JHF Laptop Zone", pageWidth / 2f, pageHeight - 16f, Paint(textPaint).apply { textAlign = Paint.Align.CENTER })

        pdf.finishPage(page)
        val outFile = File(reportDir(context), "JHF_Due_Report_${Formatters.todayDhakaIso()}.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        return outFile
    }

    fun generateSalesReportPdf(
        context: Context,
        periodLabel: String,
        sales: List<SaleRecord>
    ): File {
        val pageWidth = 842
        val pageHeight = 595
        val margin = 30f
        val pdf = PdfDocument()

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var c = page.canvas

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
        }
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val darkFill = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val whiteBold = Paint(boldPaint).apply { color = Color.WHITE }

        var y = 42f
        c.drawText("JHF Laptop Zone", pageWidth / 2f, y, headerPaint)
        y += 14f
        c.drawText("SALES REPORT - $periodLabel", pageWidth / 2f, y, Paint(boldPaint).apply { textAlign = Paint.Align.CENTER })
        y += 16f

        val colWidths = floatArrayOf(130f, 95f, 80f, 75f, 75f, 60f, 95f, 92f, 80f)
        val headers = listOf("Model", "Serial", "Sale Date", "Sell Price", "Profit", "Margin%", "Customer", "Invoice", "Staff")

        fun drawHeader(atY: Float): Float {
            val totalW = colWidths.sum()
            c.drawRect(margin, atY, margin + totalW, atY + 20f, darkFill)
            c.drawRect(margin, atY, margin + totalW, atY + 20f, borderPaint)
            var x = margin
            headers.forEachIndexed { idx, h ->
                c.drawText(h, x + 4f, atY + 13.5f, whiteBold)
                x += colWidths[idx]
            }
            return atY + 20f
        }

        y = drawHeader(y)
        sales.forEach { s ->
            if (y > pageHeight - 70f) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                c = page.canvas
                y = drawHeader(36f)
            }
            val rowH = 20f
            val totalW = colWidths.sum()
            c.drawRect(margin, y, margin + totalW, y + rowH, borderPaint)
            val marginPct = if (s.sellPrice > 0) String.format(java.util.Locale.US, "%.1f%%", (s.profit / s.sellPrice) * 100.0) else "0%"
            var x = margin
            listOf(
                (s.model ?: "Laptop").take(23),
                (s.serialNumber ?: "").take(16),
                Formatters.formatDateBD(s.saleDate).take(12),
                Formatters.formatNumber(s.sellPrice),
                Formatters.formatNumber(s.profit),
                marginPct,
                s.customerName.take(16),
                s.invoiceNumber.take(16),
                (s.staffName ?: s.staffId.toString()).take(14)
            ).forEachIndexed { idx, v ->
                c.drawText(v, x + 4f, y + 13.5f, textPaint)
                x += colWidths[idx]
                c.drawLine(x, y, x, y + rowH, borderPaint)
            }
            y += rowH
        }

        val totalSales = sales.sumOf { it.sellPrice }
        val totalProfit = sales.sumOf { it.profit }
        y += 18f
        c.drawText("Total Sold Units: ${sales.size} | Total Sales: ${Formatters.formatTaka(totalSales)} | Total Profit: ${Formatters.formatTaka(totalProfit)}", margin, y, boldPaint)
        c.drawText("Software Developed By Jahid Hasan | JHF Laptop Zone", pageWidth / 2f, pageHeight - 16f, Paint(textPaint).apply { textAlign = Paint.Align.CENTER })

        pdf.finishPage(page)
        val outFile = File(reportDir(context), "JHF_Sales_Report_${periodLabel}.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        return outFile
    }

    fun generateSupplierEntryReportPdf(
        context: Context,
        entry: SupplierReportEntry,
        startDate: String,
        endDate: String
    ): File {
        val pageWidth = 842
        val pageHeight = 595
        val margin = 30f
        val pdf = PdfDocument()
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        val c = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
        }
        val linePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        var y = 42f
        c.drawText("JHF Laptop Zone - Supplier Report", pageWidth / 2f, y, titlePaint)
        y += 20f
        c.drawText("Supplier: ${entry.name}", margin, y, boldPaint)
        y += 14f
        c.drawText("Date Range: ${startDate.ifBlank { "All" }} to ${endDate.ifBlank { "All" }}", margin, y, textPaint)
        y += 14f
        val due = (entry.totalPurchase + entry.openingBalance) - entry.totalPaid
        c.drawText(
            "Total Purchase: ${Formatters.formatTaka(entry.totalPurchase)} | Total Paid: ${Formatters.formatTaka(entry.totalPaid)} | Remaining Due: ${Formatters.formatTaka(due)}",
            margin,
            y,
            boldPaint
        )
        y += 20f

        c.drawText("Purchase History (${entry.purchases.size})", margin, y, boldPaint)
        y += 14f
        entry.purchases.take(10).forEach { p ->
            c.drawText(
                "${p.purchaseDate} | ${p.model ?: "Laptop"} (${p.serialNumber ?: ""}) - ${Formatters.formatTaka(p.purchaseAmount)}",
                margin + 8f,
                y,
                textPaint
            )
            y += 13f
        }
        y += 12f
        c.drawText("Payment History (${entry.payments.size})", margin, y, boldPaint)
        y += 14f
        entry.payments.take(10).forEach { pay ->
            c.drawText(
                "${pay.paymentDate} | ${pay.paymentType} | ${pay.note ?: "-"} - ${Formatters.formatTaka(pay.paymentAmount)}",
                margin + 8f,
                y,
                textPaint
            )
            y += 13f
        }

        c.drawLine(margin, pageHeight - 36f, pageWidth - margin, pageHeight - 36f, linePaint)
        c.drawText("Software Developed By Jahid Hasan | JHF Laptop Zone", pageWidth / 2f, pageHeight - 16f, Paint(textPaint).apply { textAlign = Paint.Align.CENTER })

        pdf.finishPage(page)
        val safeName = entry.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(reportDir(context), "JHF_Supplier_Report_${safeName}_${Formatters.todayDhakaIso()}.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        return outFile
    }

    fun generateAllSuppliersSummaryPdf(context: Context, suppliers: List<SupplierItem>): File {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 30f
        val pdf = PdfDocument()
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        val c = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
        }
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }

        var y = 44f
        c.drawText("JHF Laptop Zone", pageWidth / 2f, y, titlePaint)
        y += 14f
        c.drawText("Supplier Full Summary Statement | ${Formatters.nowDhakaDateTime()}", pageWidth / 2f, y, Paint(textPaint).apply { textAlign = Paint.Align.CENTER })
        y += 18f

        val colWidths = floatArrayOf(155f, 95f, 95f, 95f, 95f)
        val headers = listOf("Supplier", "Mobile", "Purchase", "Paid", "Due")
        c.drawRect(margin, y, margin + colWidths.sum(), y + 20f, borderPaint)
        var hx = margin
        headers.forEachIndexed { idx, h ->
            c.drawText(h, hx + 4f, y + 13.5f, boldPaint)
            hx += colWidths[idx]
            c.drawLine(hx, y, hx, y + 20f, borderPaint)
        }
        y += 20f

        suppliers.take(30).forEach { s ->
            val rowH = 20f
            c.drawRect(margin, y, margin + colWidths.sum(), y + rowH, borderPaint)
            var x = margin
            listOf(
                s.name.take(24),
                (s.mobile ?: "N/A").take(14),
                Formatters.formatNumber(s.totalPurchase),
                Formatters.formatNumber(s.totalPaid),
                Formatters.formatNumber(s.due)
            ).forEachIndexed { idx, v ->
                c.drawText(v, x + 4f, y + 13.5f, textPaint)
                x += colWidths[idx]
                c.drawLine(x, y, x, y + rowH, borderPaint)
            }
            y += rowH
        }

        val grandDue = suppliers.sumOf { it.due }
        y += 20f
        c.drawText("Grand Total Outstanding Due: ${Formatters.formatTaka(grandDue)}", margin, y, Paint(boldPaint).apply { textSize = 12f })
        c.drawText("Software Developed By Jahid Hasan | JHF Laptop Zone", pageWidth / 2f, pageHeight - 18f, Paint(textPaint).apply { textAlign = Paint.Align.CENTER })

        pdf.finishPage(page)
        val outFile = File(reportDir(context), "Supplier_Full_Report_${Formatters.todayDhakaIso()}.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        return outFile
    }
}
