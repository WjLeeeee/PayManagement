package com.woojin.paymanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.utils.PaydayAdjustment

@Composable
fun PaydaySetupScreen(
    onSetupComplete: (payday: Int, adjustment: PaydayAdjustment) -> Unit
) {
    var selectedPayday by remember { mutableStateOf(25) }
    var selectedAdjustment by remember { mutableStateOf(PaydayAdjustment.BEFORE_WEEKEND) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Title
        Text(
            text = "월급날 설정",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "월급날을 선택하시면 해당 날짜 기준으로\n한 달 단위로 관리됩니다",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Payday Selection
        Text(
            text = "월급날을 선택해주세요",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items((1..31).toList()) { day ->
                PaydaySelectionItem(
                    day = day,
                    isSelected = selectedPayday == day,
                    onClick = { selectedPayday = day }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Weekend/Holiday Adjustment
        Text(
            text = "월급날이 주말/공휴일인 경우",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            modifier = Modifier.selectableGroup()
        ) {
            PaydayAdjustment.values().forEach { adjustment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (adjustment == selectedAdjustment),
                            onClick = { selectedAdjustment = adjustment },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (adjustment == selectedAdjustment),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.Gray
                        )
                    )
                    Text(
                        text = when (adjustment) {
                            PaydayAdjustment.BEFORE_WEEKEND -> "이전 평일에 지급"
                            PaydayAdjustment.AFTER_WEEKEND -> "이후 평일에 지급"
                        },
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Complete Button
        Button(
            onClick = {
                onSetupComplete(selectedPayday, selectedAdjustment)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.LightGray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "설정 완료",
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PaydaySelectionItem(
    day: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() }
            .clip(CircleShape)
            .background(
                if (isSelected) Color.Gray else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Gray else Color.LightGray,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}