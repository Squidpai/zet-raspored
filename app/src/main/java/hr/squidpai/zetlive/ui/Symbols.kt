package hr.squidpai.zetlive.ui

import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Symbols {

  private inline fun symbol(
    name: String,
    defaultSize: Dp = 24.dp,
    viewportSize: Float = 960f,
    autoMirror: Boolean = false,
    pathBuilder: PathBuilder.() -> Unit
  ) = ImageVector.Builder(
    name = name,
    defaultWidth = defaultSize,
    defaultHeight = defaultSize,
    viewportWidth = viewportSize,
    viewportHeight = viewportSize,
    autoMirror = autoMirror,
  ).apply { materialPath(pathBuilder = pathBuilder) }.build()

  val SwapHorizontal = symbol("SwapHorizontal") {
    moveTo(280f, 800f)
    lineTo(80f, 600f)
    lineTo(280f, 400f)
    lineTo(336f, 457f)
    lineTo(233f, 560f)
    lineTo(520f, 560f)
    lineTo(520f, 640f)
    lineTo(233f, 640f)
    lineTo(336f, 743f)
    lineTo(280f, 800f)
    close()
    moveTo(680f, 560f)
    lineTo(624f, 503f)
    lineTo(727f, 400f)
    lineTo(440f, 400f)
    lineTo(440f, 320f)
    lineTo(727f, 320f)
    lineTo(624f, 217f)
    lineTo(680f, 160f)
    lineTo(880f, 360f)
    lineTo(680f, 560f)
    close()
  }

  val PushPin = symbol("PushPin") {
    moveTo(640f, 480f)
    lineTo(720f, 560f)
    lineTo(720f, 640f)
    lineTo(520f, 640f)
    lineTo(520f, 880f)
    lineTo(480f, 920f)
    lineTo(440f, 880f)
    lineTo(440f, 640f)
    lineTo(240f, 640f)
    lineTo(240f, 560f)
    lineTo(320f, 480f)
    lineTo(320f, 200f)
    lineTo(280f, 200f)
    lineTo(280f, 120f)
    lineTo(680f, 120f)
    lineTo(680f, 200f)
    lineTo(640f, 200f)
    lineTo(640f, 480f)
    close()
    moveTo(354f, 560f)
    lineTo(606f, 560f)
    lineTo(560f, 514f)
    lineTo(560f, 200f)
    lineTo(400f, 200f)
    lineTo(400f, 514f)
    lineTo(354f, 560f)
    close()
    moveTo(480f, 560f)
    lineTo(480f, 560f)
    lineTo(480f, 560f)
    lineTo(480f, 560f)
    lineTo(480f, 560f)
    lineTo(480f, 560f)
    lineTo(480f, 560f)
    close()
  }

  val PushPinFilled = symbol("Filled.PushPin") {
    moveTo(640f, 480f)
    lineTo(720f, 560f)
    lineTo(720f, 640f)
    lineTo(520f, 640f)
    lineTo(520f, 880f)
    lineTo(480f, 920f)
    lineTo(440f, 880f)
    lineTo(440f, 640f)
    lineTo(240f, 640f)
    lineTo(240f, 560f)
    lineTo(320f, 480f)
    lineTo(320f, 200f)
    lineTo(280f, 200f)
    lineTo(280f, 120f)
    lineTo(680f, 120f)
    lineTo(680f, 200f)
    lineTo(640f, 200f)
    lineTo(640f, 480f)
    close()
  }

  val Search = symbol("Search") {
    moveTo(784f, 840f)
    lineTo(532f, 588f)
    quadTo(502f, 612f, 463f, 626f)
    quadTo(424f, 640f, 380f, 640f)
    quadTo(271f, 640f, 195.5f, 564.5f)
    quadTo(120f, 489f, 120f, 380f)
    quadTo(120f, 271f, 195.5f, 195.5f)
    quadTo(271f, 120f, 380f, 120f)
    quadTo(489f, 120f, 564.5f, 195.5f)
    quadTo(640f, 271f, 640f, 380f)
    quadTo(640f, 424f, 626f, 463f)
    quadTo(612f, 502f, 588f, 532f)
    lineTo(840f, 784f)
    lineTo(784f, 840f)
    close()
    moveTo(380f, 560f)
    quadTo(455f, 560f, 507.5f, 507.5f)
    quadTo(560f, 455f, 560f, 380f)
    quadTo(560f, 305f, 507.5f, 252.5f)
    quadTo(455f, 200f, 380f, 200f)
    quadTo(305f, 200f, 252.5f, 252.5f)
    quadTo(200f, 305f, 200f, 380f)
    quadTo(200f, 455f, 252.5f, 507.5f)
    quadTo(305f, 560f, 380f, 560f)
    close()
  }

  val Close = symbol("Close") {
    moveTo(256f, 760f)
    lineTo(200f, 704f)
    lineTo(424f, 480f)
    lineTo(200f, 256f)
    lineTo(256f, 200f)
    lineTo(480f, 424f)
    lineTo(704f, 200f)
    lineTo(760f, 256f)
    lineTo(536f, 480f)
    lineTo(760f, 704f)
    lineTo(704f, 760f)
    lineTo(480f, 536f)
    lineTo(256f, 760f)
    close()
  }

  val ArrowRightAlt = symbol("ArrowRightAlt") {
    moveTo(646f, 520f)
    lineTo(200f, 520f)
    quadTo(183f, 520f, 171.5f, 508.5f)
    quadTo(160f, 497f, 160f, 480f)
    quadTo(160f, 463f, 171.5f, 451.5f)
    quadTo(183f, 440f, 200f, 440f)
    lineTo(646f, 440f)
    lineTo(532f, 326f)
    quadTo(520f, 314f, 520.5f, 298f)
    quadTo(521f, 282f, 532f, 270f)
    quadTo(544f, 258f, 560.5f, 257.5f)
    quadTo(577f, 257f, 589f, 269f)
    lineTo(772f, 452f)
    quadTo(778f, 458f, 780.5f, 465f)
    quadTo(783f, 472f, 783f, 480f)
    quadTo(783f, 488f, 780.5f, 495f)
    quadTo(778f, 502f, 772f, 508f)
    lineTo(589f, 691f)
    quadTo(577f, 703f, 560.5f, 702.5f)
    quadTo(544f, 702f, 532f, 690f)
    quadTo(521f, 678f, 520.5f, 662f)
    quadTo(520f, 646f, 532f, 634f)
    lineTo(646f, 520f)
    close()
  }

  val ArrowLeftAlt = symbol("ArrowLeftAlt") {
    moveTo(314f, 520f)
    lineTo(428f, 634f)
    quadTo(440f, 646f, 439.5f, 662f)
    quadTo(439f, 678f, 428f, 690f)
    quadTo(416f, 702f, 399.5f, 702.5f)
    quadTo(383f, 703f, 371f, 691f)
    lineTo(188f, 508f)
    quadTo(176f, 496f, 176f, 480f)
    quadTo(176f, 464f, 188f, 452f)
    lineTo(371f, 269f)
    quadTo(383f, 257f, 399.5f, 257.5f)
    quadTo(416f, 258f, 428f, 270f)
    quadTo(439f, 282f, 439.5f, 298f)
    quadTo(440f, 314f, 428f, 326f)
    lineTo(314f, 440f)
    lineTo(760f, 440f)
    quadTo(777f, 440f, 788.5f, 451.5f)
    quadTo(800f, 463f, 800f, 480f)
    quadTo(800f, 497f, 788.5f, 508.5f)
    quadTo(777f, 520f, 760f, 520f)
    lineTo(314f, 520f)
    close()
  }

  val ArrowUpwardAlt = symbol("ArrowUpwardAlt") {
    moveTo(440f, 352f)
    lineTo(324f, 468f)
    quadTo(313f, 479f, 296f, 479f)
    quadTo(279f, 479f, 268f, 468f)
    quadTo(257f, 457f, 257f, 440f)
    quadTo(257f, 423f, 268f, 412f)
    lineTo(452f, 228f)
    quadTo(464f, 216f, 480f, 216f)
    quadTo(496f, 216f, 508f, 228f)
    lineTo(692f, 412f)
    quadTo(703f, 423f, 703f, 440f)
    quadTo(703f, 457f, 692f, 468f)
    quadTo(681f, 479f, 664f, 479f)
    quadTo(647f, 479f, 636f, 468f)
    lineTo(520f, 352f)
    lineTo(520f, 680f)
    quadTo(520f, 697f, 508.5f, 708.5f)
    quadTo(497f, 720f, 480f, 720f)
    quadTo(463f, 720f, 451.5f, 708.5f)
    quadTo(440f, 697f, 440f, 680f)
    lineTo(440f, 352f)
    close()
  }

  val ArrowDownwardAlt = symbol("ArrowDownwardAlt") {
    moveTo(440f, 568f)
    lineTo(440f, 240f)
    quadTo(440f, 223f, 451.5f, 211.5f)
    quadTo(463f, 200f, 480f, 200f)
    quadTo(497f, 200f, 508.5f, 211.5f)
    quadTo(520f, 223f, 520f, 240f)
    lineTo(520f, 568f)
    lineTo(636f, 452f)
    quadTo(647f, 441f, 664f, 441f)
    quadTo(681f, 441f, 692f, 452f)
    quadTo(703f, 463f, 703f, 480f)
    quadTo(703f, 497f, 692f, 508f)
    lineTo(508f, 692f)
    quadTo(496f, 704f, 480f, 704f)
    quadTo(464f, 704f, 452f, 692f)
    lineTo(268f, 508f)
    quadTo(257f, 497f, 257f, 480f)
    quadTo(257f, 463f, 268f, 452f)
    quadTo(279f, 441f, 296f, 441f)
    quadTo(313f, 441f, 324f, 452f)
    lineTo(440f, 568f)
    close()
  }

  val Tram20 = symbol("Tram:20", defaultSize = 20.dp) {
    moveTo(192f, 696f)
    lineTo(192f, 336f)
    quadTo(192f, 269f, 253f, 230.5f)
    quadTo(314f, 192f, 457f, 192f)
    lineTo(481f, 144f)
    lineTo(288f, 144f)
    lineTo(288f, 96f)
    lineTo(672f, 96f)
    lineTo(672f, 144f)
    lineTo(535f, 144f)
    lineTo(511f, 192f)
    quadTo(656f, 192f, 712f, 231f)
    quadTo(768f, 270f, 768f, 336f)
    lineTo(768f, 696f)
    quadTo(768f, 750f, 733f, 790f)
    quadTo(698f, 830f, 645f, 838f)
    lineTo(696f, 888f)
    lineTo(696f, 912f)
    lineTo(618f, 912f)
    lineTo(546f, 840f)
    lineTo(414f, 840f)
    lineTo(342f, 912f)
    lineTo(264f, 912f)
    lineTo(264f, 888f)
    lineTo(314f, 838f)
    quadTo(261f, 830f, 226.5f, 790f)
    quadTo(192f, 750f, 192f, 696f)
    close()
    moveTo(641f, 576f)
    lineTo(318f, 576f)
    quadTo(295f, 576f, 279.5f, 576f)
    quadTo(264f, 576f, 264f, 576f)
    lineTo(264f, 576f)
    lineTo(696f, 576f)
    lineTo(696f, 576f)
    quadTo(696f, 576f, 680f, 576f)
    quadTo(664f, 576f, 641f, 576f)
    close()
    moveTo(480f, 720f)
    quadTo(500f, 720f, 514f, 706f)
    quadTo(528f, 692f, 528f, 672f)
    quadTo(528f, 652f, 514f, 638f)
    quadTo(500f, 624f, 480f, 624f)
    quadTo(460f, 624f, 446f, 638f)
    quadTo(432f, 652f, 432f, 672f)
    quadTo(432f, 692f, 446f, 706f)
    quadTo(460f, 720f, 480f, 720f)
    close()
    moveTo(478f, 312f)
    quadTo(606f, 312f, 643.5f, 312f)
    quadTo(681f, 312f, 692f, 312f)
    lineTo(269f, 312f)
    quadTo(280f, 312f, 316f, 312f)
    quadTo(352f, 312f, 478f, 312f)
    close()
    moveTo(264f, 504f)
    lineTo(696f, 504f)
    lineTo(696f, 384f)
    lineTo(264f, 384f)
    lineTo(264f, 504f)
    close()
    moveTo(336f, 768f)
    lineTo(624f, 768f)
    quadTo(654f, 768f, 675f, 747f)
    quadTo(696f, 726f, 696f, 696f)
    lineTo(696f, 576f)
    lineTo(264f, 576f)
    lineTo(264f, 696f)
    quadTo(264f, 726f, 285f, 747f)
    quadTo(306f, 768f, 336f, 768f)
    close()
    moveTo(480f, 264f)
    quadTo(388f, 264f, 340.5f, 273.5f)
    quadTo(293f, 283f, 269f, 312f)
    lineTo(692f, 312f)
    quadTo(667f, 284f, 628f, 274f)
    quadTo(589f, 264f, 480f, 264f)
    close()
  }

  val Bus20 = symbol("Bus:20", defaultSize = 20.dp) {
    moveTo(336f, 744f)
    lineTo(336f, 768f)
    quadTo(336f, 788f, 322f, 802f)
    quadTo(308f, 816f, 288f, 816f)
    quadTo(268f, 816f, 254f, 802f)
    quadTo(240f, 788f, 240f, 768f)
    lineTo(240f, 707f)
    quadTo(217f, 688f, 204.5f, 660f)
    quadTo(192f, 632f, 192f, 600f)
    lineTo(192f, 240f)
    quadTo(192f, 168f, 250f, 132f)
    quadTo(308f, 96f, 480f, 96f)
    quadTo(651f, 96f, 709.5f, 132f)
    quadTo(768f, 168f, 768f, 240f)
    lineTo(768f, 600f)
    quadTo(768f, 632f, 755.5f, 660f)
    quadTo(743f, 688f, 720f, 707f)
    lineTo(720f, 768f)
    quadTo(720f, 788f, 706f, 802f)
    quadTo(692f, 816f, 672f, 816f)
    quadTo(652f, 816f, 638f, 802f)
    quadTo(624f, 788f, 624f, 768f)
    lineTo(624f, 744f)
    lineTo(336f, 744f)
    close()
    moveTo(482.18f, 216f)
    quadTo(582.09f, 216f, 627.04f, 216f)
    quadTo(672f, 216f, 692f, 216f)
    lineTo(269f, 216f)
    quadTo(294f, 216f, 339.5f, 216f)
    quadTo(385f, 216f, 482.18f, 216f)
    close()
    moveTo(624f, 480f)
    lineTo(336f, 480f)
    quadTo(307f, 480f, 285.5f, 480f)
    quadTo(264f, 480f, 264f, 480f)
    lineTo(264f, 480f)
    lineTo(696f, 480f)
    lineTo(696f, 480f)
    quadTo(696f, 480f, 674.85f, 480f)
    quadTo(653.7f, 480f, 624f, 480f)
    close()
    moveTo(264f, 408f)
    lineTo(696f, 408f)
    lineTo(696f, 288f)
    lineTo(264f, 288f)
    lineTo(264f, 408f)
    close()
    moveTo(360f, 624f)
    quadTo(380f, 624f, 394f, 610f)
    quadTo(408f, 596f, 408f, 576f)
    quadTo(408f, 556f, 394f, 542f)
    quadTo(380f, 528f, 360f, 528f)
    quadTo(340f, 528f, 326f, 542f)
    quadTo(312f, 556f, 312f, 576f)
    quadTo(312f, 596f, 326f, 610f)
    quadTo(340f, 624f, 360f, 624f)
    close()
    moveTo(600f, 624f)
    quadTo(620f, 624f, 634f, 610f)
    quadTo(648f, 596f, 648f, 576f)
    quadTo(648f, 556f, 634f, 542f)
    quadTo(620f, 528f, 600f, 528f)
    quadTo(580f, 528f, 566f, 542f)
    quadTo(552f, 556f, 552f, 576f)
    quadTo(552f, 596f, 566f, 610f)
    quadTo(580f, 624f, 600f, 624f)
    close()
    moveTo(269f, 216f)
    lineTo(692f, 216f)
    quadTo(672f, 187f, 626f, 177.5f)
    quadTo(580f, 168f, 480f, 168f)
    quadTo(387f, 168f, 339.5f, 178f)
    quadTo(292f, 188f, 269f, 216f)
    close()
    moveTo(336.05f, 672f)
    lineTo(624.28f, 672f)
    quadTo(654f, 672f, 675f, 650.85f)
    quadTo(696f, 629.7f, 696f, 600f)
    lineTo(696f, 480f)
    lineTo(264f, 480f)
    lineTo(264f, 600f)
    quadTo(264f, 630f, 285.17f, 651f)
    quadTo(306.33f, 672f, 336.05f, 672f)
    close()
  }

  val Schedule = symbol("Schedule") {
    moveTo(612f, 668f)
    lineTo(668f, 612f)
    lineTo(520f, 464f)
    lineTo(520f, 280f)
    lineTo(440f, 280f)
    lineTo(440f, 496f)
    lineTo(612f, 668f)
    close()
    moveTo(480f, 880f)
    quadTo(397f, 880f, 324f, 848.5f)
    quadTo(251f, 817f, 197f, 763f)
    quadTo(143f, 709f, 111.5f, 636f)
    quadTo(80f, 563f, 80f, 480f)
    quadTo(80f, 397f, 111.5f, 324f)
    quadTo(143f, 251f, 197f, 197f)
    quadTo(251f, 143f, 324f, 111.5f)
    quadTo(397f, 80f, 480f, 80f)
    quadTo(563f, 80f, 636f, 111.5f)
    quadTo(709f, 143f, 763f, 197f)
    quadTo(817f, 251f, 848.5f, 324f)
    quadTo(880f, 397f, 880f, 480f)
    quadTo(880f, 563f, 848.5f, 636f)
    quadTo(817f, 709f, 763f, 763f)
    quadTo(709f, 817f, 636f, 848.5f)
    quadTo(563f, 880f, 480f, 880f)
    close()
    moveTo(480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    close()
    moveTo(480f, 800f)
    quadTo(613f, 800f, 706.5f, 706.5f)
    quadTo(800f, 613f, 800f, 480f)
    quadTo(800f, 347f, 706.5f, 253.5f)
    quadTo(613f, 160f, 480f, 160f)
    quadTo(347f, 160f, 253.5f, 253.5f)
    quadTo(160f, 347f, 160f, 480f)
    quadTo(160f, 613f, 253.5f, 706.5f)
    quadTo(347f, 800f, 480f, 800f)
    close()
  }

  val ClockLoader10 = symbol("ClockLoader10") {
    moveTo(480f, 880f)
    quadTo(397f, 880f, 324f, 848.5f)
    quadTo(251f, 817f, 197f, 763f)
    quadTo(143f, 709f, 111.5f, 636f)
    quadTo(80f, 563f, 80f, 480f)
    quadTo(80f, 397f, 111.5f, 324f)
    quadTo(143f, 251f, 197f, 197f)
    quadTo(251f, 143f, 324f, 111.5f)
    quadTo(397f, 80f, 480f, 80f)
    quadTo(563f, 80f, 636f, 111.5f)
    quadTo(709f, 143f, 763f, 197f)
    quadTo(817f, 251f, 848.5f, 324f)
    quadTo(880f, 397f, 880f, 480f)
    quadTo(880f, 563f, 848.5f, 636f)
    quadTo(817f, 709f, 763f, 763f)
    quadTo(709f, 817f, 636f, 848.5f)
    quadTo(563f, 880f, 480f, 880f)
    close()
    moveTo(480f, 800f)
    quadTo(614f, 800f, 707f, 707f)
    quadTo(800f, 614f, 800f, 480f)
    quadTo(800f, 416f, 776f, 357f)
    quadTo(752f, 298f, 707f, 253f)
    lineTo(480f, 480f)
    lineTo(480f, 160f)
    quadTo(346f, 160f, 253f, 253f)
    quadTo(160f, 346f, 160f, 480f)
    quadTo(160f, 614f, 253f, 707f)
    quadTo(346f, 800f, 480f, 800f)
    close()
  }

  @Suppress("ObjectPropertyName")
  val _360 = symbol("360") {
    moveTo(360f, 800f)
    lineTo(304f, 744f)
    lineTo(374f, 672f)
    quadTo(246f, 655f, 163f, 602f)
    quadTo(80f, 549f, 80f, 480f)
    quadTo(80f, 397f, 195.5f, 338.5f)
    quadTo(311f, 280f, 480f, 280f)
    quadTo(649f, 280f, 764.5f, 338.5f)
    quadTo(880f, 397f, 880f, 480f)
    quadTo(880f, 542f, 813.5f, 591f)
    quadTo(747f, 640f, 640f, 664f)
    lineTo(640f, 582f)
    quadTo(717f, 562f, 758.5f, 532.5f)
    quadTo(800f, 503f, 800f, 480f)
    quadTo(800f, 448f, 714.5f, 404f)
    quadTo(629f, 360f, 480f, 360f)
    quadTo(331f, 360f, 245.5f, 404f)
    quadTo(160f, 448f, 160f, 480f)
    quadTo(160f, 504f, 211f, 537.5f)
    quadTo(262f, 571f, 356f, 588f)
    lineTo(304f, 536f)
    lineTo(360f, 480f)
    lineTo(520f, 640f)
    lineTo(360f, 800f)
    close()
  }

  val Settings = symbol("Settings") {
    moveTo(370f, 880f)
    lineTo(354f, 752f)
    quadTo(341f, 747f, 329.5f, 740f)
    quadTo(318f, 733f, 307f, 725f)
    lineTo(188f, 775f)
    lineTo(78f, 585f)
    lineTo(181f, 507f)
    quadTo(180f, 500f, 180f, 493.5f)
    quadTo(180f, 487f, 180f, 480f)
    quadTo(180f, 473f, 180f, 466.5f)
    quadTo(180f, 460f, 181f, 453f)
    lineTo(78f, 375f)
    lineTo(188f, 185f)
    lineTo(307f, 235f)
    quadTo(318f, 227f, 330f, 220f)
    quadTo(342f, 213f, 354f, 208f)
    lineTo(370f, 80f)
    lineTo(590f, 80f)
    lineTo(606f, 208f)
    quadTo(619f, 213f, 630.5f, 220f)
    quadTo(642f, 227f, 653f, 235f)
    lineTo(772f, 185f)
    lineTo(882f, 375f)
    lineTo(779f, 453f)
    quadTo(780f, 460f, 780f, 466.5f)
    quadTo(780f, 473f, 780f, 480f)
    quadTo(780f, 487f, 780f, 493.5f)
    quadTo(780f, 500f, 778f, 507f)
    lineTo(881f, 585f)
    lineTo(771f, 775f)
    lineTo(653f, 725f)
    quadTo(642f, 733f, 630f, 740f)
    quadTo(618f, 747f, 606f, 752f)
    lineTo(590f, 880f)
    lineTo(370f, 880f)
    close()
    moveTo(440f, 800f)
    lineTo(519f, 800f)
    lineTo(533f, 694f)
    quadTo(564f, 686f, 590.5f, 670.5f)
    quadTo(617f, 655f, 639f, 633f)
    lineTo(738f, 674f)
    lineTo(777f, 606f)
    lineTo(691f, 541f)
    quadTo(696f, 527f, 698f, 511.5f)
    quadTo(700f, 496f, 700f, 480f)
    quadTo(700f, 464f, 698f, 448.5f)
    quadTo(696f, 433f, 691f, 419f)
    lineTo(777f, 354f)
    lineTo(738f, 286f)
    lineTo(639f, 328f)
    quadTo(617f, 305f, 590.5f, 289.5f)
    quadTo(564f, 274f, 533f, 266f)
    lineTo(520f, 160f)
    lineTo(441f, 160f)
    lineTo(427f, 266f)
    quadTo(396f, 274f, 369.5f, 289.5f)
    quadTo(343f, 305f, 321f, 327f)
    lineTo(222f, 286f)
    lineTo(183f, 354f)
    lineTo(269f, 418f)
    quadTo(264f, 433f, 262f, 448f)
    quadTo(260f, 463f, 260f, 480f)
    quadTo(260f, 496f, 262f, 511f)
    quadTo(264f, 526f, 269f, 541f)
    lineTo(183f, 606f)
    lineTo(222f, 674f)
    lineTo(321f, 632f)
    quadTo(343f, 655f, 369.5f, 670.5f)
    quadTo(396f, 686f, 427f, 694f)
    lineTo(440f, 800f)
    close()
    moveTo(482f, 620f)
    quadTo(540f, 620f, 581f, 579f)
    quadTo(622f, 538f, 622f, 480f)
    quadTo(622f, 422f, 581f, 381f)
    quadTo(540f, 340f, 482f, 340f)
    quadTo(423f, 340f, 382.5f, 381f)
    quadTo(342f, 422f, 342f, 480f)
    quadTo(342f, 538f, 382.5f, 579f)
    quadTo(423f, 620f, 482f, 620f)
    close()
    moveTo(480f, 480f)
    lineTo(480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    lineTo(480f, 480f)
    lineTo(480f, 480f)
    lineTo(480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    lineTo(480f, 480f)
    lineTo(480f, 480f)
    lineTo(480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    lineTo(480f, 480f)
    lineTo(480f, 480f)
    lineTo(480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    lineTo(480f, 480f)
    lineTo(480f, 480f)
    lineTo(480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    lineTo(480f, 480f)
    lineTo(480f, 480f)
    lineTo(480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    lineTo(480f, 480f)
    close()
  }

  val MoreVert = symbol("MoreVert") {
    moveTo(480f, 800f)
    quadTo(447f, 800f, 423.5f, 776.5f)
    quadTo(400f, 753f, 400f, 720f)
    quadTo(400f, 687f, 423.5f, 663.5f)
    quadTo(447f, 640f, 480f, 640f)
    quadTo(513f, 640f, 536.5f, 663.5f)
    quadTo(560f, 687f, 560f, 720f)
    quadTo(560f, 753f, 536.5f, 776.5f)
    quadTo(513f, 800f, 480f, 800f)
    close()
    moveTo(480f, 560f)
    quadTo(447f, 560f, 423.5f, 536.5f)
    quadTo(400f, 513f, 400f, 480f)
    quadTo(400f, 447f, 423.5f, 423.5f)
    quadTo(447f, 400f, 480f, 400f)
    quadTo(513f, 400f, 536.5f, 423.5f)
    quadTo(560f, 447f, 560f, 480f)
    quadTo(560f, 513f, 536.5f, 536.5f)
    quadTo(513f, 560f, 480f, 560f)
    close()
    moveTo(480f, 320f)
    quadTo(447f, 320f, 423.5f, 296.5f)
    quadTo(400f, 273f, 400f, 240f)
    quadTo(400f, 207f, 423.5f, 183.5f)
    quadTo(447f, 160f, 480f, 160f)
    quadTo(513f, 160f, 536.5f, 183.5f)
    quadTo(560f, 207f, 560f, 240f)
    quadTo(560f, 273f, 536.5f, 296.5f)
    quadTo(513f, 320f, 480f, 320f)
    close()
  }

  val MyLocation = symbol("MyLocation") {
    moveTo(440f, 918f)
    lineTo(440f, 838f)
    quadTo(315f, 824f, 225.5f, 734.5f)
    quadTo(136f, 645f, 122f, 520f)
    lineTo(42f, 520f)
    lineTo(42f, 440f)
    lineTo(122f, 440f)
    quadTo(136f, 315f, 225.5f, 225.5f)
    quadTo(315f, 136f, 440f, 122f)
    lineTo(440f, 42f)
    lineTo(520f, 42f)
    lineTo(520f, 122f)
    quadTo(645f, 136f, 734.5f, 225.5f)
    quadTo(824f, 315f, 838f, 440f)
    lineTo(918f, 440f)
    lineTo(918f, 520f)
    lineTo(838f, 520f)
    quadTo(824f, 645f, 734.5f, 734.5f)
    quadTo(645f, 824f, 520f, 838f)
    lineTo(520f, 918f)
    lineTo(440f, 918f)
    close()
    moveTo(480f, 760f)
    quadTo(596f, 760f, 678f, 678f)
    quadTo(760f, 596f, 760f, 480f)
    quadTo(760f, 364f, 678f, 282f)
    quadTo(596f, 200f, 480f, 200f)
    quadTo(364f, 200f, 282f, 282f)
    quadTo(200f, 364f, 200f, 480f)
    quadTo(200f, 596f, 282f, 678f)
    quadTo(364f, 760f, 480f, 760f)
    close()
    moveTo(480f, 640f)
    quadTo(414f, 640f, 367f, 593f)
    quadTo(320f, 546f, 320f, 480f)
    quadTo(320f, 414f, 367f, 367f)
    quadTo(414f, 320f, 480f, 320f)
    quadTo(546f, 320f, 593f, 367f)
    quadTo(640f, 414f, 640f, 480f)
    quadTo(640f, 546f, 593f, 593f)
    quadTo(546f, 640f, 480f, 640f)
    close()
    moveTo(480f, 560f)
    quadTo(513f, 560f, 536.5f, 536.5f)
    quadTo(560f, 513f, 560f, 480f)
    quadTo(560f, 447f, 536.5f, 423.5f)
    quadTo(513f, 400f, 480f, 400f)
    quadTo(447f, 400f, 423.5f, 423.5f)
    quadTo(400f, 447f, 400f, 480f)
    quadTo(400f, 513f, 423.5f, 536.5f)
    quadTo(447f, 560f, 480f, 560f)
    close()
    moveTo(480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    quadTo(480f, 480f, 480f, 480f)
    close()
  }

  val NoInternet = symbol("NoInternet") {
    moveTo(480f, 880f)
    quadTo(398f, 880f, 325f, 848.5f)
    quadTo(252f, 817f, 197.5f, 762.5f)
    quadTo(143f, 708f, 111.5f, 635f)
    quadTo(80f, 562f, 80f, 480f)
    quadTo(80f, 397f, 111.5f, 324.5f)
    quadTo(143f, 252f, 197.5f, 197.5f)
    quadTo(252f, 143f, 325f, 111.5f)
    quadTo(398f, 80f, 480f, 80f)
    quadTo(563f, 80f, 635.5f, 111.5f)
    quadTo(708f, 143f, 762.5f, 197.5f)
    quadTo(817f, 252f, 848.5f, 324.5f)
    quadTo(880f, 397f, 880f, 480f)
    lineTo(880f, 520.5f)
    lineTo(480f, 520.5f)
    close()
    moveTo(480f, 798f)
    lineTo(480f, 640f)
    lineTo(404f, 640f)
    quadTo(416f, 684f, 435f, 723f)
    quadTo(454f, 762f, 480f, 798f)
    close()
    moveTo(376f, 782f)
    quadTo(358f, 749f, 344.5f, 713.5f)
    quadTo(331f, 678f, 322f, 640f)
    lineTo(204f, 640f)
    quadTo(233f, 690f, 276.5f, 727f)
    quadTo(320f, 764f, 376f, 782f)
    close()
    moveTo(170f, 560f)
    lineTo(306f, 560f)
    quadTo(303f, 540f, 301.5f, 520.5f)
    quadTo(300f, 501f, 300f, 480f)
    quadTo(300f, 459f, 301.5f, 439.5f)
    quadTo(303f, 420f, 306f, 400f)
    lineTo(170f, 400f)
    quadTo(165f, 420f, 162.5f, 439.5f)
    quadTo(160f, 459f, 160f, 480f)
    quadTo(160f, 501f, 162.5f, 520.5f)
    quadTo(165f, 540f, 170f, 560f)
    close()
    moveTo(386f, 560f)
    lineTo(480f, 560f)
    lineTo(480f, 520.5f)
    lineTo(578.5f, 520.5f)
    quadTo(580f, 501f, 580f, 480f)
    quadTo(580f, 459f, 578.5f, 439.5f)
    quadTo(577f, 420f, 574f, 400f)
    lineTo(386f, 400f)
    quadTo(383f, 420f, 381.5f, 439.5f)
    quadTo(380f, 459f, 380f, 480f)
    quadTo(380f, 501f, 381.5f, 520.5f)
    quadTo(383f, 540f, 386f, 560f)
    close()
    moveTo(658.5f, 520.5f)
    lineTo(797.5f, 520.5f)
    quadTo(800f, 501f, 800f, 480f)
    quadTo(800f, 459f, 797.5f, 439.5f)
    quadTo(795f, 420f, 790f, 400f)
    lineTo(654f, 400f)
    quadTo(657f, 420f, 658.5f, 439.5f)
    quadTo(660f, 459f, 660f, 480f)
    close()
    moveTo(638f, 320f)
    lineTo(756f, 320f)
    quadTo(727f, 270f, 683.5f, 233f)
    quadTo(640f, 196f, 584f, 178f)
    quadTo(602f, 211f, 615.5f, 246.5f)
    quadTo(629f, 282f, 638f, 320f)
    close()
    moveTo(404f, 320f)
    lineTo(556f, 320f)
    quadTo(544f, 276f, 525f, 237f)
    quadTo(506f, 198f, 480f, 162f)
    quadTo(454f, 198f, 435f, 237f)
    quadTo(416f, 276f, 404f, 320f)
    close()
    moveTo(204f, 320f)
    lineTo(322f, 320f)
    quadTo(331f, 282f, 344.5f, 246.5f)
    quadTo(358f, 211f, 376f, 178f)
    quadTo(320f, 196f, 276.5f, 233f)
    quadTo(233f, 270f, 204f, 320f)
    close()
    moveTo(596f, 860.5f)
    lineToRelative(-56f, -56f)
    lineToRelative(84f, -84f)
    lineToRelative(-84f, -84f)
    lineToRelative(56f, -56f)
    lineToRelative(84f, 84f)
    lineToRelative(84f, -84f)
    lineToRelative(57f, 56f)
    lineToRelative(-84f, 84f)
    lineToRelative(83f, 84f)
    lineToRelative(-56f, 56f)
    lineToRelative(-84f, -83f)
    close()
  }

  val Upgrade = symbol("Upgrade") {
    moveTo(280f, 800f)
    lineTo(280f, 720f)
    lineTo(680f, 720f)
    lineTo(680f, 800f)
    lineTo(280f, 800f)
    close()
    moveTo(440f, 640f)
    lineTo(440f, 313f)
    lineTo(336f, 416f)
    lineTo(280f, 360f)
    lineTo(480f, 160f)
    lineTo(680f, 360f)
    lineTo(624f, 416f)
    lineTo(520f, 313f)
    lineTo(520f, 640f)
    lineTo(440f, 640f)
    close()
  }

}