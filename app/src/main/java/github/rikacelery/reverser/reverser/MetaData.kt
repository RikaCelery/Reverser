package github.rikacelery.reverser.reverser

data class MetaData(
    val blockSize: Long,
    val createdDate: Long,
    val noFileName: Boolean,
    val originalFileName: String = "",
) {
    companion object {
    }
}
