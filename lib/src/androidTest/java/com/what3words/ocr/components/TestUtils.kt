package com.what3words.ocr.components

import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Autosuggest
import com.what3words.javawrapper.response.Suggestion
import retrofit2.Response

class TestData {

    companion object {
        val rinoLinoFinoAutosuggestResponse = Autosuggest(emptyList()).apply {
            val response = APIResponse<Autosuggest>(null)
            response.error = APIResponse.What3WordsError.BAD_WORDS
            this.setResponse(response)
        }

        val invalidBulgarianAutosuggestResponse = Autosuggest(emptyList()).apply {
            val response = APIResponse<Autosuggest>(null)
            response.error = APIResponse.What3WordsError.BAD_WORDS
            this.setResponse(response)
        }

        val invalidEnglishAutosuggestResponse = Autosuggest(emptyList()).apply {
            val response = APIResponse<Autosuggest>(null)
            response.error = APIResponse.What3WordsError.BAD_WORDS
            this.setResponse(response)
        }

        val filledCountSoapAutosuggestResponse: Autosuggest = Autosuggest(
            mutableListOf(
                Suggestion(
                    "filled.count.soap",
                    "Bayswater, London",
                    "GB",
                    0,
                    1,
                    "en"
                ),
                Suggestion(
                    "field.count.soap",
                    "Greensburg, Indiana",
                    "US",
                    0,
                    2,
                    "en"
                ),
                Suggestion(
                    "filled.kilt.soap",
                    "Chengdu, Sichuan",
                    "CN",
                    0,
                    3,
                    "en"
                )
            )
        ).apply {
            setResponse(
                APIResponse(
                    Response.success(
                        this
                    )
                )
            )
        }

        val italianAutosuggestResponse: Autosuggest = Autosuggest(
            mutableListOf(
                Suggestion(
                    "amavo.trattò.tendoni",
                    "Londra",
                    "GB",
                    0,
                    1,
                    "it"
                ),
                Suggestion(
                    "amavano.trattò.tendoni",
                    "Abra Pampa, Jujuy",
                    "AR",
                    0,
                    2,
                    "it"
                ),
                Suggestion(
                    "amavo.tratti.tendoni",
                    "Tres Isletas, Chaco",
                    "AR",
                    0,
                    3,
                    "it"
                )
            )
        ).apply {
            setResponse(
                APIResponse(
                    Response.success(
                        this
                    )
                )
            )
        }

        val portugueseAutosuggestResponse: Autosuggest = Autosuggest(
            mutableListOf(
                Suggestion(
                    "refrigerando.valem.touro",
                    "Londres, London",
                    "GB",
                    0,
                    1,
                    "pt"
                ),
                Suggestion(
                    "refrigerantes.valem.touro",
                    "São Félix do Xingu, Pará",
                    "BR",
                    0,
                    2,
                    "pt"
                ),
                Suggestion(
                    "refrigerando.valeu.touro",
                    "Coaraci, Bahia",
                    "BR",
                    0,
                    3,
                    "pt"
                )
            )
        ).apply {
            setResponse(
                APIResponse(
                    Response.success(
                        this
                    )
                )
            )
        }

        val indexHomeRaftAutosuggestResponse: Autosuggest = Autosuggest(
            mutableListOf(
                Suggestion(
                    "index.home.raft",
                    "Bayswater, London",
                    "GB",
                    0,
                    1,
                    "en"
                ),
                Suggestion(
                    "indexes.home.raft",
                    "Prosperity, West Virginia",
                    "US",
                    0,
                    2,
                    "en"
                ),
                Suggestion(
                    "index.homes.raft",
                    "Greensboro, North Carolina",
                    "US",
                    0,
                    3,
                    "en"
                )
            )
        ).apply {
            setResponse(
                APIResponse(
                    Response.success(
                        this
                    )
                )
            )
        }


        val bulgarianAutosuggestResponse: Autosuggest = Autosuggest(
            mutableListOf(
                Suggestion(
                    "щраус.банкнота.дюни",
                    "Лондон, London",
                    "GB",
                    0,
                    1,
                    "bg"
                ),
                Suggestion(
                    "щраус.банкноти.дюни",
                    "Huérmeces, Кастилия и Леон",
                    "ES",
                    0,
                    2,
                    "bg"
                ),
                Suggestion(
                    "щраус.банкнота.дюли",
                    "Kitahiroshima, Hokkaido",
                    "JP",
                    0,
                    3,
                    "bg"
                )
            )
        ).apply {
            setResponse(
                APIResponse(
                    Response.success(
                        this
                    )
                )
            )
        }

        val hindiAutosuggestResponse: Autosuggest = Autosuggest(
            mutableListOf(
                Suggestion(
                    "डोलने.पीसना.संभाला",
                    "लंदन, London",
                    "GB",
                    0,
                    1,
                    "hi"
                ),
                Suggestion(
                    "रचाते.बहू.कसने",
                    "Koryazhma, आर्कान्जेस्क ओब्लास्ट",
                    "RU",
                    0,
                    2,
                    "hi"
                ),
                Suggestion(
                    "चराते.बहू.सकने",
                    "Motomiya, फ़ूकूशिमा प्रीफ़ेक्चर",
                    "JP",
                    0,
                    3,
                    "hi"
                )
            )
        ).apply {
            setResponse(
                APIResponse(
                    Response.success(
                        this
                    )
                )
            )
        }

        val japaneseAutosuggestResponse: Autosuggest = Autosuggest(
            mutableListOf(
                Suggestion(
                    "こくさい。ていか。かざす",
                    "ロンドン, London",
                    "GB",
                    0,
                    1,
                    "ja"
                ),
                Suggestion(
                    "はくさい。ていか。かざす",
                    "ウドムルト共和国, ヴォトキンスク",
                    "RU",
                    0,
                    2,
                    "ja"
                ),
                Suggestion(
                    "こくさい。ついか。かざす",
                    "中部ビサヤ地方, ラプ＝ラプ市",
                    "PH",
                    0,
                    3,
                    "ja"
                )
            )
        ).apply {
            setResponse(
                APIResponse(
                    Response.success(
                        this
                    )
                )
            )
        }

        val koreanAutosuggestResponse: Autosuggest = Autosuggest(
            mutableListOf(
                Suggestion(
                    "쓸모.평소.나다",
                    "런던, London",
                    "GB",
                    0,
                    1,
                    "ko"
                ),
                Suggestion(
                    "쓸모.평수.나다",
                    "Oxford, 캔터베리 지방",
                    "NZ",
                    0,
                    2,
                    "ko"
                ),
                Suggestion(
                    "쓸모.평소.자다",
                    "베를린",
                    "DE",
                    0,
                    3,
                    "ko"
                )
            )
        ).apply {
            setResponse(
                APIResponse(
                    Response.success(
                        this
                    )
                )
            )
        }

        val chineseAutosuggestResponse: Autosuggest = Autosuggest(
            mutableListOf(
                Suggestion(
                    "产权.绝缘.墨镜",
                    "伦敦, London",
                    "GB",
                    0,
                    1,
                    "zh"
                ),
                Suggestion(
                    "山泉.绝缘.墨镜",
                    "Kaliua, Tabora",
                    "TZ",
                    0,
                    2,
                    "zh"
                ),
                Suggestion(
                    "产前.绝缘.墨镜",
                    "姆班达卡, Équateur",
                    "CG",
                    0,
                    3,
                    "zh"
                )
            )
        ).apply {
            setResponse(
                APIResponse(
                    Response.success(
                        this
                    )
                )
            )
        }
    }
}