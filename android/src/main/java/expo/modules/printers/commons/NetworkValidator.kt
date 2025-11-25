package expo.modules.printers.commons

object NetworkValidator {
    private val IP_ADDRESS_PATTERN = Regex(
        "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    )

    fun isValidIpAddress(ipAddress: String): Boolean {
        return IP_ADDRESS_PATTERN.matches(ipAddress)
    }

    fun isValidPort(port: Int): Boolean {
        return port in 1..65535
    }

    fun validateNetworkConnection(ipAddress: String, port: Int): ValidationResult {
        if (ipAddress.isBlank()) {
            return ValidationResult.Error("IP address cannot be empty")
        }

        if (!isValidIpAddress(ipAddress)) {
            return ValidationResult.Error("Invalid IP address format: $ipAddress")
        }

        if (!isValidPort(port)) {
            return ValidationResult.Error("Port must be between 1 and 65535, got: $port")
        }

        return ValidationResult.Valid
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
