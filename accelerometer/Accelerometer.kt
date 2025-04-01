package dev.ricknout.composesensors.demo.ui.accelerometer

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.ricknout.composesensors.accelerometer.isAccelerometerSensorAvailable
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState
import dev.ricknout.composesensors.demo.model.Demo
import dev.ricknout.composesensors.demo.ui.Demo
import dev.ricknout.composesensors.demo.ui.NotAvailableDemo



// Función para reflejar horizontalmente un Rect (respecto a centerX)
fun mirrorRectHorizontally(rect: Rect, centerX: Float): Rect {
    val dxLeft = centerX - rect.left
    val dxRight = rect.right - centerX
    return Rect(
        left = centerX - dxRight,
        top = rect.top,
        right = centerX + dxLeft,
        bottom = rect.bottom
    )
}

// Función para reflejar verticalmente un Rect (respecto a centerY)
fun mirrorRectVertically(rect: Rect, centerY: Float): Rect {
    val dyTop = centerY - rect.top
    val dyBottom = rect.bottom - centerY
    return Rect(
        left = rect.left,
        top = centerY - dyBottom,
        right = rect.right,
        bottom = centerY + dyTop
    )
}

/**
 * Genera una “mini-grid” de obstáculos en el **cuadrante superior-izquierdo** con `rows` filas y `columns` columnas.
 * - hSpacing y vSpacing controlan el espacio horizontal/vertical entre obstáculos.
 * - obstacleWidth y obstacleHeight controlan el tamaño de cada obstáculo.
 * - startX y startY indican dónde arranca la grilla.
 */
fun generateTopLeftGridIntercalated(
    rows: Int,
    columns: Int,
    startX: Float,
    startY: Float,
    obstacleWidth: Float,
    obstacleHeight: Float,
    hSpacing: Float,
    vSpacing: Float
): List<Rect> {
    val obstacles = mutableListOf<Rect>()
    for (row in 0 until rows) {
        for (col in 0 until columns) {
            // Intercalación: incluir solo si la suma de fila y columna es par
            if ((row + col) % 2 == 0) {
                val left = startX + col * (obstacleWidth + hSpacing)
                val top = startY + row * (obstacleHeight + vSpacing)
                obstacles.add(
                    Rect(
                        left = left,
                        top = top,
                        right = left + obstacleWidth,
                        bottom = top + obstacleHeight
                    )
                )
            }
        }
    }
    return obstacles
}

@Composable
fun AccelerometerDemo() {
    if (isAccelerometerSensorAvailable()) {
        val sensorValue by rememberAccelerometerSensorValueAsState()
        val (x, y, _) = sensorValue.value
        Demo(
            demo = Demo.ACCELEROMETER,
            value = ""
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val orientation = LocalConfiguration.current.orientation
            val contentColor = LocalContentColor.current

            // Bola más pequeña
            val ballRadius = with(LocalDensity.current) { 6.dp.toPx() }

            // Movimiento
            var center by remember { mutableStateOf(Offset(width / 2, height / 2)) }
            var velocity by remember { mutableStateOf(Offset.Zero) }
            var redScore by remember { mutableStateOf(0) }
            var blueScore by remember { mutableStateOf(0) }

            val factor = 0.25f
            val damping = 0.98f
            val acceleration = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                Offset(-x, y)
            } else {
                Offset(y, x)
            }
            velocity *= damping
            velocity += acceleration * factor
            var newCenter = center + velocity

            // Rebote en bordes
            if (newCenter.x < ballRadius) {
                newCenter = newCenter.copy(x = ballRadius)
                velocity = velocity.copy(x = -velocity.x)
            } else if (newCenter.x > width - ballRadius) {
                newCenter = newCenter.copy(x = width - ballRadius)
                velocity = velocity.copy(x = -velocity.x)
            }
            if (newCenter.y < ballRadius) {
                newCenter = newCenter.copy(y = ballRadius)
                velocity = velocity.copy(y = -velocity.y)
            } else if (newCenter.y > height - ballRadius) {
                newCenter = newCenter.copy(y = height - ballRadius)
                velocity = velocity.copy(y = -velocity.y)
            }

            // Porterías (para goles)
            val goalHeight = with(LocalDensity.current) { 40.dp.toPx() }
            val goalWidth = width / 3f
            val goalLeft = (width - goalWidth) / 2f
            val blueGoalRect = Rect(goalLeft, 0f, goalLeft + goalWidth, goalHeight)
            val redGoalRect = Rect(goalLeft, height - goalHeight, goalLeft + goalWidth, height)

            // Goles
            if ((newCenter.y - ballRadius) <= blueGoalRect.bottom &&
                newCenter.x in blueGoalRect.left..blueGoalRect.right
            ) {
                redScore++
                newCenter = Offset(width / 2, height / 2)
                velocity = Offset.Zero
            } else if ((newCenter.y + ballRadius) >= redGoalRect.top &&
                newCenter.x in redGoalRect.left..redGoalRect.right
            ) {
                blueScore++
                newCenter = Offset(width / 2, height / 2)
                velocity = Offset.Zero
            }

            // Protecciones de portería (gris)
            val thickness = with(LocalDensity.current) { 10.dp.toPx() }
            val gapFraction = 0.2f
            val gapWidth = goalWidth * gapFraction
            val marginFront = thickness * 0.5f

            val blueObsLeft = Rect(
                goalLeft,
                blueGoalRect.bottom + marginFront,
                goalLeft + (goalWidth - gapWidth) / 2f,
                blueGoalRect.bottom + marginFront + thickness
            )
            val blueObsRight = Rect(
                goalLeft + (goalWidth + gapWidth) / 2f,
                blueGoalRect.bottom + marginFront,
                goalLeft + goalWidth,
                blueGoalRect.bottom + marginFront + thickness
            )
            val blueSideLeft = Rect(
                blueGoalRect.left - thickness,
                blueGoalRect.top,
                blueGoalRect.left,
                blueGoalRect.bottom
            )
            val blueSideRight = Rect(
                blueGoalRect.right,
                blueGoalRect.top,
                blueGoalRect.right + thickness,
                blueGoalRect.bottom
            )
            val redObsLeft = Rect(
                goalLeft,
                redGoalRect.top - marginFront - thickness,
                goalLeft + (goalWidth - gapWidth) / 2f,
                redGoalRect.top - marginFront
            )
            val redObsRight = Rect(
                goalLeft + (goalWidth + gapWidth) / 2f,
                redGoalRect.top - marginFront - thickness,
                goalLeft + goalWidth,
                redGoalRect.top - marginFront
            )
            val redSideLeft = Rect(
                redGoalRect.left - thickness,
                redGoalRect.top,
                redGoalRect.left,
                redGoalRect.bottom
            )
            val redSideRight = Rect(
                redGoalRect.right,
                redGoalRect.top,
                redGoalRect.right + thickness,
                redGoalRect.bottom
            )
            val protectionObstacles = listOf(
                blueObsLeft, blueObsRight, blueSideLeft, blueSideRight,
                redObsLeft, redObsRight, redSideLeft, redSideRight
            )

            // ============= Generar una "mini-grid" en el cuadrante superior-izquierdo =============
            val rows = 8
            val columns = 4
            // Posición de inicio de la grilla
            val startX = width * 0.05f
            val startY = height * 0.1f
            // Tamaño de cada obstáculo
            val obstacleWidth = width * 0.07f
            val obstacleHeight = thickness
            // Espacio horizontal y vertical entre obstáculos
            val hSpacing = width * 0.05f
            val vSpacing = height * 0.04f

            // Generar la grid en el cuadrante superior-izquierdo con intercalación
            val topLeftGrid = generateTopLeftGridIntercalated(
                rows = rows,
                columns = columns,
                startX = startX,
                startY = startY,
                obstacleWidth = obstacleWidth,
                obstacleHeight = obstacleHeight,
                hSpacing = hSpacing,
                vSpacing = vSpacing
            )

            // Reflejar horizontalmente para tener la parte superior-derecha
            val centerX = width / 2f
            val topRightGrid = topLeftGrid.map { mirrorRectHorizontally(it, centerX) }

            // Reflejar verticalmente para llenar la parte inferior
            val centerY = height / 2f
            val bottomLeftGrid = topLeftGrid.map { mirrorRectVertically(it, centerY) }
            val bottomRightGrid = topRightGrid.map { mirrorRectVertically(it, centerY) }

            // Combinar todo: top-left, top-right, bottom-left, bottom-right
            val additionalObstacles = topLeftGrid + topRightGrid + bottomLeftGrid + bottomRightGrid

            // ============= Añadir otra lista de obstáculos =============
            val anotherObstacles = mutableListOf<Rect>()
            val anotherObstacleWidth = obstacleWidth * 1.5f
            val anotherObstacleHeight = obstacleHeight * 1.2f
            val centerYPos = height / 2f
            val horizontalCenter = width / 2f // Calculate the horizontal center

            // Move the green obstacles closer to the center


            // Finalmente, combina protecciones + grid + otra lista
            val totalObstacles = protectionObstacles + additionalObstacles + anotherObstacles

            // Detectar colisión
            fun checkCollision(rect: Rect) {
                if (newCenter.x + ballRadius > rect.left &&
                    newCenter.x - ballRadius < rect.right &&
                    newCenter.y + ballRadius > rect.top &&
                    newCenter.y - ballRadius < rect.bottom
                ) {
                    val overlapLeft = newCenter.x + ballRadius - rect.left
                    val overlapRight = rect.right - (newCenter.x - ballRadius)
                    val overlapTop = newCenter.y + ballRadius - rect.top
                    val overlapBottom = rect.bottom - (newCenter.y - ballRadius)
                    val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)
                    when (minOverlap) {
                        overlapLeft -> {
                            newCenter = newCenter.copy(x = rect.left - ballRadius)
                            velocity = velocity.copy(x = -velocity.x)
                        }
                        overlapRight -> {
                            newCenter = newCenter.copy(x = rect.right + ballRadius)
                            velocity = velocity.copy(x = -velocity.x)
                        }
                        overlapTop -> {
                            newCenter = newCenter.copy(y = rect.top - ballRadius)
                            velocity = velocity.copy(y = -velocity.y)
                        }
                        else -> {
                            newCenter = newCenter.copy(y = rect.bottom + ballRadius)
                            velocity = velocity.copy(y = -velocity.y)
                        }
                    }
                }
            }
            totalObstacles.forEach { rect -> checkCollision(rect) }
            center = newCenter

            // Dibujo final
            Column(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.weight(1f)) {
                    // Dibujar portería azul
                    drawRect(
                        color = Color.Blue.copy(alpha = 0.3f),
                        topLeft = blueGoalRect.topLeft,
                        size = androidx.compose.ui.geometry.Size(blueGoalRect.width, blueGoalRect.height)
                    )
                    // Dibujar portería roja
                    drawRect(
                        color = Color.Red.copy(alpha = 0.3f),
                        topLeft = redGoalRect.topLeft,
                        size = androidx.compose.ui.geometry.Size(redGoalRect.width, redGoalRect.height)
                    )
                    // Dibujar protecciones en gris
                    protectionObstacles.forEach { rect ->
                        drawRect(
                            color = Color.Gray.copy(alpha = 0.7f),
                            topLeft = rect.topLeft,
                            size = androidx.compose.ui.geometry.Size(rect.width, rect.height)
                        )
                    }
                    // Dibujar la grid adicional (en rojo)
                    additionalObstacles.forEach { rect ->
                        drawRect(
                            color = Color.Red.copy(alpha = 0.5f),
                            topLeft = rect.topLeft,
                            size = androidx.compose.ui.geometry.Size(rect.width, rect.height)
                        )
                    }
                    // Dibujar la otra lista de obstáculos (en verde para diferenciarlos)
                    anotherObstacles.forEach { rect ->
                        drawRect(
                            color = Color.Green.copy(alpha = 0.7f),
                            topLeft = rect.topLeft,
                            size = androidx.compose.ui.geometry.Size(rect.width, rect.height)
                        )
                    }
                    // Dibujar la bola
                    drawCircle(
                        color = contentColor,
                        radius = ballRadius,
                        center = center
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Blue: $blueScore", color = Color.Blue)
                    Text(text = "Red: $redScore", color = Color.Red)
                }
            }
        }
    } else {
        NotAvailableDemo(demo = Demo.ACCELEROMETER)
    }
}
