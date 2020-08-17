package com.libre.alexa.utils

class ApiConstants {

    companion object {
        val BASE_URL = "https://apistagingref.developer.vodafone.com/oauth2/"
        var LIBRE_CLOUD_BASE_URL = "https://cloud.librewireless.com/"
    }


    val Successful_Request: Int = 200

    val Missing_OR_Invalid_Parameters = 400

    val URI_Does_Not_Represent_A_Recognised_Resource = 404

    val Request_Method_Not_Supported = 405

    val Header_Invalid = 406

    val Payload_Exceeds_max_size = 413

    val Request_URI_too_Long = 414

    val Value_Of_The_Content_Type_Header_Invalid = 415

    val Semantic_Errors = 422

    val Api_Quota_Limit_Reached = 429

    val Server_Error = 500




}