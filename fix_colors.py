import sys

with open('app/src/main/java/com/example/ui/screens/QuizDialog.kt', 'r', encoding='utf-8') as f:
    code = f.read()

# 1. Update Card Border and Elevation
old_card_surface = """                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF0284C7)), // Highlighted Blue Border
                                    shadowElevation = 4.dp
                                )"""
new_card_surface = """                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)), // Premium Subtle Border
                                    shadowElevation = 2.dp
                                )"""
code = code.replace(old_card_surface, new_card_surface)

# 2. Update AIR text color
old_air = """                                                            Text(
                                                                text = "#AIR $airRank",
                                                                fontSize = 24.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFF0284C7)
                                                            )"""
new_air = """                                                            Text(
                                                                text = "#AIR $airRank",
                                                                fontSize = 24.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFF2563EB) // Royal Blue
                                                            )"""
code = code.replace(old_air, new_air)

# 3. Update Percentile color
old_percentile = """                                                        Text(
                                                            text = "$formattedPercentile%",
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0284C7)
                                                        )"""
new_percentile = """                                                        Text(
                                                            text = "$formattedPercentile%",
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF2563EB)
                                                        )"""
code = code.replace(old_percentile, new_percentile)

# 4. Update Clickable Strip
old_strip = """                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFF0F9FF),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Leaderboard,
                                                            contentDescription = null,
                                                            tint = Color(0xFF0284C7),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = if (showAllIndiaDetails) "कट-ऑफ, टॉपर्स लिस्ट व सेक्शनल स्कोर छुपाएं ▲" else "🎯 कट-ऑफ, टॉपर्स लिस्ट व सेक्शनल स्कोर देखें ▼",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0284C7)
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = if (showAllIndiaDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = Color(0xFF0284C7),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }"""
new_strip = """                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFF8FAFC),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Leaderboard,
                                                            contentDescription = null,
                                                            tint = Color(0xFF3B82F6),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = if (showAllIndiaDetails) "कट-ऑफ, टॉपर्स लिस्ट व सेक्शनल स्कोर छुपाएं ▲" else "🎯 कट-ऑफ, टॉपर्स लिस्ट व सेक्शनल स्कोर देखें ▼",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF334155) // Dark Slate
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = if (showAllIndiaDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = Color(0xFF334155),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }"""
code = code.replace(old_strip, new_strip)

# 5. Fix Aspirant Highlight Card in Leaderboard (Color 0xFF0284C7 to 0xFF2563EB)
old_highlight = """                                                        Surface(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color(0xFFF0F9FF),
                                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "#AIR $airRank",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = Color(0xFF0284C7),
                                                                    modifier = Modifier.width(42.dp)
                                                                )
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = "⭐ आप (You - Aspirant)",
                                                                        fontSize = 12.5.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF0284C7)
                                                                    )
                                                                    Text(
                                                                        text = "All India Rank | $formattedPercentile%ile",
                                                                        fontSize = 10.sp,
                                                                        color = Color(0xFF64748B)
                                                                    )
                                                                }
                                                                Text(
                                                                    text = "$formattedScore M",
                                                                    fontSize = 12.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF0284C7),
                                                                    modifier = Modifier.width(52.dp)
                                                                )
                                                                Text(
                                                                    text = "$accuracyPercent%",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF0284C7),
                                                                    modifier = Modifier.width(44.dp)
                                                                )
                                                            }
                                                        }"""
new_highlight = """                                                        Surface(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color(0xFFF0F9FF), // Very soft blue bg
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "#AIR $airRank",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = Color(0xFF2563EB),
                                                                    modifier = Modifier.width(42.dp)
                                                                )
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = "⭐ आप (You)",
                                                                        fontSize = 12.5.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF1E293B)
                                                                    )
                                                                    Text(
                                                                        text = "All India Rank | $formattedPercentile%ile",
                                                                        fontSize = 10.sp,
                                                                        color = Color(0xFF64748B)
                                                                    )
                                                                }
                                                                Text(
                                                                    text = "$formattedScore M",
                                                                    fontSize = 12.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF2563EB),
                                                                    modifier = Modifier.width(52.dp)
                                                                )
                                                                Text(
                                                                    text = "$accuracyPercent%",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF2563EB),
                                                                    modifier = Modifier.width(44.dp)
                                                                )
                                                            }
                                                        }"""
code = code.replace(old_highlight, new_highlight)

with open('app/src/main/java/com/example/ui/screens/QuizDialog.kt', 'w', encoding='utf-8') as f:
    f.write(code)

print("Colors updated!")
