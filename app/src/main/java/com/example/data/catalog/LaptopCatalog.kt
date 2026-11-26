package com.example.data.catalog

data class LaptopPreset(
    val model: String,
    val config: String
)

object LaptopCatalog {
    val items: List<LaptopPreset> = listOf(
        // HP EliteBook Series
        LaptopPreset("HP EliteBook 840 G3", "Core i5 6th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("HP EliteBook 840 G4", "Core i5 7th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("HP EliteBook 840 G5", "Core i5 8th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("HP EliteBook 840 G6", "Core i5 8th Gen, 16GB RAM, 256GB NVMe SSD"),
        LaptopPreset("HP EliteBook 840 G7", "Core i5 10th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP EliteBook 840 G8", "Core i5 11th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP EliteBook 840 G9", "Core i5 12th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP EliteBook 830 G5", "Core i5 8th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("HP EliteBook 830 G6", "Core i5 8th Gen, 16GB RAM, 256GB NVMe SSD"),
        LaptopPreset("HP EliteBook 830 G7", "Core i5 10th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP EliteBook 850 G5", "Core i7 8th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP EliteBook 850 G6", "Core i7 8th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP EliteBook 850 G7", "Core i7 10th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP EliteBook x360 1030 G2", "Core i5 7th Gen, 8GB RAM, 256GB SSD, Touch 360"),
        LaptopPreset("HP EliteBook x360 1030 G3", "Core i5 8th Gen, 8GB RAM, 256GB NVMe SSD, Touch 360"),
        LaptopPreset("HP EliteBook x360 1030 G4", "Core i7 8th Gen, 16GB RAM, 512GB NVMe SSD, Touch 360"),
        LaptopPreset("HP EliteBook x360 1040 G5", "Core i7 8th Gen, 16GB RAM, 512GB NVMe SSD, Touch 360"),
        LaptopPreset("HP EliteBook x360 1040 G7", "Core i7 10th Gen, 16GB RAM, 512GB NVMe SSD, Touch 360"),

        // HP ProBook Series
        LaptopPreset("HP ProBook 450 G3", "Core i5 6th Gen, 8GB RAM, 1TB HDD / 128GB SSD"),
        LaptopPreset("HP ProBook 450 G4", "Core i5 7th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("HP ProBook 450 G5", "Core i5 8th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("HP ProBook 450 G6", "Core i5 8th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("HP ProBook 450 G7", "Core i5 10th Gen, 8GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP ProBook 450 G8", "Core i5 11th Gen, 8GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP ProBook 450 G9", "Core i5 12th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP ProBook 440 G5", "Core i5 8th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("HP ProBook 440 G6", "Core i5 8th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("HP ProBook 440 G7", "Core i5 10th Gen, 8GB RAM, 512GB NVMe SSD"),
        LaptopPreset("HP ProBook 440 G8", "Core i5 11th Gen, 8GB RAM, 512GB NVMe SSD"),

        // HP Pavilion & Victus & Omen
        LaptopPreset("HP Victus 15", "Core i5 12th Gen, 16GB RAM, 512GB NVMe SSD, RTX 3050 4GB"),
        LaptopPreset("HP Victus 16", "Ryzen 7 6800H, 16GB RAM, 512GB NVMe SSD, RTX 3060 6GB"),
        LaptopPreset("HP Pavilion 15", "Core i5 11th Gen, 8GB RAM, 512GB NVMe SSD, Iris Xe"),
        LaptopPreset("HP Pavilion Gaming 15", "Ryzen 5 4600H, 8GB RAM, 512GB SSD, GTX 1650 4GB"),
        LaptopPreset("HP Omen 16", "Core i7 13th Gen, 16GB RAM, 1TB NVMe SSD, RTX 4060 8GB"),
        LaptopPreset("HP Envy x360 13", "Ryzen 5 5500U, 8GB RAM, 512GB NVMe SSD, Touch 360"),
        LaptopPreset("HP Envy x360 15", "Core i7 11th Gen, 16GB RAM, 512GB NVMe SSD, Touch 360"),

        // Dell Latitude Series
        LaptopPreset("Dell Latitude 7480", "Core i5 6th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("Dell Latitude 7490", "Core i5 8th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Dell Latitude 7400", "Core i5 8th Gen, 16GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Dell Latitude 7410", "Core i5 10th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Dell Latitude 7420", "Core i5 11th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Dell Latitude 5480", "Core i5 6th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("Dell Latitude 5490", "Core i5 8th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("Dell Latitude 5400", "Core i5 8th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Dell Latitude 5410", "Core i5 10th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Dell Latitude 5420", "Core i5 11th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Dell Latitude 5580", "Core i5 7th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("Dell Latitude 5590", "Core i7 8th Gen, 16GB RAM, 512GB SSD"),
        LaptopPreset("Dell Latitude 7390 2-in-1", "Core i5 8th Gen, 8GB RAM, 256GB SSD, Touch 360"),
        LaptopPreset("Dell Latitude 7300", "Core i7 8th Gen, 16GB RAM, 512GB SSD"),

        // Dell XPS & Inspiron & G-Series
        LaptopPreset("Dell XPS 13 9360", "Core i5 7th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Dell XPS 13 9370", "Core i7 8th Gen, 16GB RAM, 512GB NVMe SSD, 4K"),
        LaptopPreset("Dell XPS 13 9380", "Core i7 8th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Dell XPS 13 9310", "Core i7 11th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Dell XPS 15 9570", "Core i7 8th Gen, 16GB RAM, 512GB SSD, GTX 1050Ti 4GB"),
        LaptopPreset("Dell XPS 15 7590", "Core i7 9th Gen, 16GB RAM, 512GB SSD, GTX 1650 4GB"),
        LaptopPreset("Dell XPS 15 9500", "Core i7 10th Gen, 16GB RAM, 1TB SSD, GTX 1650Ti 4GB"),
        LaptopPreset("Dell Inspiron 15 3511", "Core i3 11th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Dell Inspiron 15 3520", "Core i5 12th Gen, 8GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Dell Inspiron 15 5502", "Core i5 11th Gen, 8GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Dell G15 5511", "Core i5 11th Gen, 16GB RAM, 512GB SSD, RTX 3050 4GB"),
        LaptopPreset("Dell G15 5520", "Core i7 12th Gen, 16GB RAM, 512GB SSD, RTX 3060 6GB"),

        // Lenovo ThinkPad Series
        LaptopPreset("Lenovo ThinkPad T460", "Core i5 6th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("Lenovo ThinkPad T470", "Core i5 6th/7th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("Lenovo ThinkPad T470s", "Core i5 7th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad T480", "Core i5 8th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad T480s", "Core i5 8th Gen, 16GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad T490", "Core i5 8th Gen, 16GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad T490s", "Core i7 8th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad T14 Gen 1", "Core i5 10th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad T14 Gen 2", "Core i5 11th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad T14 Gen 3", "Core i5 12th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad X260", "Core i5 6th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("Lenovo ThinkPad X270", "Core i5 7th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("Lenovo ThinkPad X280", "Core i5 8th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad X390", "Core i5 8th Gen, 16GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad X13 Gen 1", "Core i5 10th Gen, 16GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad X1 Carbon Gen 5", "Core i7 7th Gen, 16GB RAM, 512GB SSD"),
        LaptopPreset("Lenovo ThinkPad X1 Carbon Gen 6", "Core i7 8th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad X1 Carbon Gen 7", "Core i7 8th/10th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad X1 Carbon Gen 8", "Core i7 10th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad X1 Carbon Gen 9", "Core i7 11th Gen, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Lenovo ThinkPad X1 Yoga Gen 3", "Core i7 8th Gen, 16GB RAM, 512GB SSD, Touch 360"),
        LaptopPreset("Lenovo ThinkPad X1 Yoga Gen 4", "Core i7 8th Gen, 16GB RAM, 512GB SSD, Touch 360"),
        LaptopPreset("Lenovo ThinkPad L480", "Core i5 8th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("Lenovo ThinkPad L380 Yoga", "Core i5 8th Gen, 8GB RAM, 256GB SSD, Touch 360"),

        // Lenovo IdeaPad & Legion
        LaptopPreset("Lenovo IdeaPad Slim 3", "Core i3 11th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Lenovo IdeaPad Slim 5", "Ryzen 5 5500U, 16GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Lenovo IdeaPad Gaming 3", "Ryzen 5 5600H, 16GB RAM, 512GB SSD, RTX 3050 4GB"),
        LaptopPreset("Lenovo Legion 5", "Ryzen 7 5800H, 16GB RAM, 512GB SSD, RTX 3060 6GB"),
        LaptopPreset("Lenovo Legion Pro 5", "Core i7 13th Gen, 16GB RAM, 1TB NVMe SSD, RTX 4060 8GB"),

        // Apple MacBooks
        LaptopPreset("MacBook Air 13 (2017)", "Core i5 1.8GHz, 8GB RAM, 128GB/256GB SSD"),
        LaptopPreset("MacBook Air Retina (2018)", "Core i5 8th Gen, 8GB RAM, 128GB/256GB SSD"),
        LaptopPreset("MacBook Air Retina (2019)", "Core i5 8th Gen, 8GB RAM, 128GB/256GB SSD"),
        LaptopPreset("MacBook Air Retina (2020)", "Core i3/i5 10th Gen, 8GB RAM, 256GB SSD"),
        LaptopPreset("MacBook Air M1 (2020)", "Apple M1 8-Core, 8GB RAM, 256GB SSD"),
        LaptopPreset("MacBook Air M2 (2022)", "Apple M2 8-Core, 8GB RAM, 256GB SSD, Liquid Retina"),
        LaptopPreset("MacBook Air M3 (2024)", "Apple M3 8-Core, 8GB RAM, 256GB SSD"),
        LaptopPreset("MacBook Pro 13 (2017)", "Core i5 7th Gen, 8GB RAM, 256GB SSD, Non-Touch Bar"),
        LaptopPreset("MacBook Pro 13 (2018)", "Core i5 Quad-Core, 8GB RAM, 256GB SSD, Touch Bar"),
        LaptopPreset("MacBook Pro 13 (2019)", "Core i5 Quad-Core, 8GB RAM, 256GB SSD, Touch Bar"),
        LaptopPreset("MacBook Pro 13 (2020)", "Core i5 10th Gen, 16GB RAM, 512GB SSD, Touch Bar"),
        LaptopPreset("MacBook Pro 13 M1 (2020)", "Apple M1 8-Core, 8GB RAM, 256GB SSD"),
        LaptopPreset("MacBook Pro 13 M2 (2022)", "Apple M2 8-Core, 8GB RAM, 256GB SSD"),
        LaptopPreset("MacBook Pro 14 M1 Pro", "Apple M1 Pro 8-Core, 16GB RAM, 512GB SSD"),
        LaptopPreset("MacBook Pro 14 M2 Pro", "Apple M2 Pro 10-Core, 16GB RAM, 512GB SSD"),
        LaptopPreset("MacBook Pro 14 M3", "Apple M3 8-Core, 8GB RAM, 512GB SSD"),
        LaptopPreset("MacBook Pro 16 M1 Pro", "Apple M1 Pro 10-Core, 16GB RAM, 512GB SSD"),

        // Asus ZenBook, VivoBook & ROG
        LaptopPreset("Asus ZenBook 14 UX425", "Core i5 11th Gen, 8GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Asus ZenBook 13 OLED", "Core i5 11th Gen, 8GB RAM, 512GB NVMe SSD, OLED"),
        LaptopPreset("Asus VivoBook 15", "Core i3 11th Gen, 8GB RAM, 256GB NVMe SSD"),
        LaptopPreset("Asus VivoBook Pro 15 OLED", "Ryzen 7 5800H, 16GB RAM, 512GB SSD, RTX 3050 4GB"),
        LaptopPreset("Asus TUF Gaming F15", "Core i5 11th Gen, 16GB RAM, 512GB SSD, RTX 3050 4GB"),
        LaptopPreset("Asus TUF Gaming A15", "Ryzen 7 6800H, 16GB RAM, 512GB SSD, RTX 3060 6GB"),
        LaptopPreset("Asus ROG Strix G15", "Ryzen 7 6800H, 16GB RAM, 512GB SSD, RTX 3060 6GB"),
        LaptopPreset("Asus ROG Strix G16", "Core i7 13th Gen, 16GB RAM, 1TB NVMe SSD, RTX 4060 8GB"),
        LaptopPreset("Asus ROG Zephyrus G14", "Ryzen 7 5800HS, 16GB RAM, 512GB SSD, RTX 3060 6GB"),

        // Acer & MSI
        LaptopPreset("Acer Aspire 5", "Core i5 11th Gen, 8GB RAM, 512GB NVMe SSD"),
        LaptopPreset("Acer Nitro 5", "Core i5 12th Gen, 16GB RAM, 512GB SSD, RTX 3050 4GB"),
        LaptopPreset("Acer Swift 3", "Ryzen 5 5500U, 8GB RAM, 512GB NVMe SSD"),
        LaptopPreset("MSI GF63 Thin", "Core i5 11th Gen, 8GB RAM, 512GB SSD, GTX 1650 / RTX 3050"),
        LaptopPreset("MSI Katana GF66", "Core i7 12th Gen, 16GB RAM, 512GB SSD, RTX 3060 6GB"),
        LaptopPreset("MSI Modern 14", "Core i5 11th Gen, 8GB RAM, 512GB NVMe SSD")
    )

    fun searchModels(input: String): List<String> {
        val q = input.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return items.filter { it.model.lowercase().contains(q) }
            .map { it.model }
            .take(8)
    }

    fun getConfig(modelName: String): String {
        val q = modelName.trim().lowercase()
        if (q.isEmpty()) return "Core i5, 8GB RAM, 256GB SSD"
        return items.firstOrNull { it.model.lowercase() == q }?.config
            ?: items.firstOrNull {
                it.model.lowercase().contains(q) || q.contains(it.model.lowercase())
            }?.config
            ?: "Core i5, 8GB RAM, 256GB SSD"
    }
}
