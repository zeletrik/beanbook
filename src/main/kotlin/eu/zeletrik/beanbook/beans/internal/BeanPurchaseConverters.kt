package eu.zeletrik.beanbook.beans.internal

import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.RoastProfile
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.core.dialect.JdbcArrayColumns
import org.springframework.data.jdbc.core.dialect.JdbcDialect
import org.springframework.data.relational.core.dialect.AnsiDialect
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

// SQLite has no native DATE, DECIMAL, or UUID types — all stored as TEXT.

@WritingConverter
class BigDecimalToStringConverter : Converter<BigDecimal, String> {
    override fun convert(source: BigDecimal): String = source.toPlainString()
}

@ReadingConverter
class StringToBigDecimalConverter : Converter<String, BigDecimal> {
    override fun convert(source: String): BigDecimal = BigDecimal(source)
}

@WritingConverter
class LocalDateToStringConverter : Converter<LocalDate, String> {
    override fun convert(source: LocalDate): String = source.toString()
}

@ReadingConverter
class StringToLocalDateConverter : Converter<String, LocalDate> {
    override fun convert(source: String): LocalDate = LocalDate.parse(source)
}

@WritingConverter
class UuidToStringConverter : Converter<UUID, String> {
    override fun convert(source: UUID): String = source.toString()
}

@ReadingConverter
class StringToUuidConverter : Converter<String, UUID> {
    override fun convert(source: String): UUID = UUID.fromString(source)
}

@WritingConverter
class RoastLevelToStringConverter : Converter<RoastLevel, String> {
    override fun convert(source: RoastLevel): String = source.name
}

@ReadingConverter
class StringToRoastLevelConverter : Converter<String, RoastLevel> {
    override fun convert(source: String): RoastLevel = RoastLevel.valueOf(source)
}

@WritingConverter
class ProcessToStringConverter : Converter<Process, String> {
    override fun convert(source: Process): String = source.name
}

@ReadingConverter
class StringToProcessConverter : Converter<String, Process> {
    override fun convert(source: String): Process = Process.valueOf(source)
}

@WritingConverter
class RoastProfileToStringConverter : Converter<RoastProfile, String> {
    override fun convert(source: RoastProfile): String = source.name
}

@ReadingConverter
class StringToRoastProfileConverter : Converter<String, RoastProfile> {
    override fun convert(source: String): RoastProfile = RoastProfile.valueOf(source)
}

@WritingConverter
class TagListToStringConverter : Converter<List<String>, String?> {
    override fun convert(source: List<String>): String? =
        source.joinToString(",").takeIf { source.isNotEmpty() }
}

@ReadingConverter
class StringToTagListConverter : Converter<String, List<String>> {
    override fun convert(source: String): List<String> =
        source.split(",").map { it.trim() }.filter { it.isNotBlank() }
}

// SQLite has no built-in dialect in Spring Data JDBC — provide AnsiDialect as the base.
// Must override getArraySupport() to resolve the ambiguity between AnsiDialect and JdbcDialect.
class SqliteJdbcDialect : AnsiDialect(), JdbcDialect {
    override fun getArraySupport(): JdbcArrayColumns = JdbcArrayColumns.Unsupported.INSTANCE
}

@Configuration
class BeanPurchaseConverters {

    @Bean
    fun jdbcDialect(): JdbcDialect = SqliteJdbcDialect()

    @Bean
    fun jdbcCustomConversions(): JdbcCustomConversions = JdbcCustomConversions(
        listOf(
            BigDecimalToStringConverter(),
            StringToBigDecimalConverter(),
            LocalDateToStringConverter(),
            StringToLocalDateConverter(),
            UuidToStringConverter(),
            StringToUuidConverter(),
            RoastLevelToStringConverter(),
            StringToRoastLevelConverter(),
            ProcessToStringConverter(),
            StringToProcessConverter(),
            RoastProfileToStringConverter(),
            StringToRoastProfileConverter(),
            TagListToStringConverter(),
            StringToTagListConverter(),
        )
    )
}
