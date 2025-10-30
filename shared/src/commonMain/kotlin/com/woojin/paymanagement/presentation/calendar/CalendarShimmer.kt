package com.woojin.paymanagement.presentation.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.presentation.components.ShimmerBox

@Composable
fun CalendarScreenShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Pay Period Summary Card Shimmer
        PayPeriodSummaryCardShimmer()

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Grid Shimmer
        CalendarGridShimmer()

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Transaction Card Shimmer
        DailyTransactionCardShimmer()

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PayPeriodSummaryCardShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            ShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(24.dp),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(3) {
                    Column(horizontalAlignment = if (it == 0) Alignment.Start else if (it == 1) Alignment.CenterHorizontally else Alignment.End) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(50.dp)
                                .height(16.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(20.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGridShimmer() {
    Column {
        // Week Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(7) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Days
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(250.dp)
        ) {
            items(35) {
                ShimmerBox(
                    modifier = Modifier
                        .size(40.dp),
                    shape = CircleShape
                )
            }
        }
    }
}

@Composable
private fun DailyTransactionCardShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            ShimmerBox(
                modifier = Modifier
                    .width(180.dp)
                    .height(24.dp),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction Items
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .size(8.dp),
                        shape = CircleShape
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(100.dp)
                                .height(16.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .width(60.dp)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }

                    ShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }

                if (it == 0) Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
