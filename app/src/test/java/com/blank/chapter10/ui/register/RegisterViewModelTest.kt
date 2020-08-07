package com.blank.chapter10.ui.register

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.blank.chapter10.data.AppDataManager
import com.blank.chapter10.data.local.db.entity.User
import com.blank.chapter10.data.model.DataRegister
import com.blank.chapter10.data.model.ResponseRegister
import com.blank.chapter10.utils.api.BaseErrorDataSourceApi
import com.blank.chapter10.utils.api.ResultState
import com.blank.teamb_ex.BodyRegister
import com.edwinnrw.moviecleanarchitecture.helper.InstantRuleExecution
import com.edwinnrw.moviecleanarchitecture.helper.TrampolineSchedulerRX
import com.google.gson.Gson
import com.jraska.livedata.test
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import retrofit2.HttpException
import retrofit2.Response

@RunWith(JUnitPlatform::class)
class RegisterViewModelTest : Spek({
    Feature("Register") {
        val appDataManager = mock<AppDataManager>()
        val saveStateHandle = mock<SavedStateHandle>()
        val viewModel = RegisterViewModel(appDataManager, saveStateHandle)
        val observerRegister = mock<Observer<ResultState>>()

        val bodyRegister = BodyRegister(
            password = "123123",
            email = "ghozimahdi@gmail.com",
            username = "ghozimahdi"
        )

        beforeFeature {
            TrampolineSchedulerRX.start()
            InstantRuleExecution.start()
            viewModel.resultStateResponseRegister.observeForever(observerRegister)
        }

        Scenario("do on register, response success") {
            val expectedResult = ResponseRegister(
                DataRegister("1", "ghozimahdi@gmail.com", "ghozimahdi"), true
            )
            var expectedResultState: ResultState

            Given("set expected result state") {
                expectedResultState = ResultState.Success(expectedResult, "")

                given(appDataManager.register(bodyRegister)).willReturn(
                    Single.just(
                        expectedResultState
                    )
                )
            }

            When("request api register") {
                viewModel.register(bodyRegister)
            }

            Then("result success and return data") {
                verify(observerRegister, atLeastOnce()).onChanged(ResultState.Loading)
                verify(
                    observerRegister,
                    atLeastOnce()
                ).onChanged(ResultState.Success(expectedResult, ""))
                verify(appDataManager).register(bodyRegister)
                viewModel.resultStateResponseRegister.hasActiveObservers()

                val ex = appDataManager.register(bodyRegister)
                    .blockingGet()
                Assert.assertNotNull(ex)
                
            }
        }

        Scenario("do on register, response error") {
            val baseDataSourceApi = BaseErrorDataSourceApi(false, "Bad Request")
            val responseBody: Response<BaseErrorDataSourceApi> =
                Response.error(400, Gson().toJson(baseDataSourceApi).toString().toResponseBody())
            val errorExpected = HttpException(responseBody)

            Given("set error expected") {
                Mockito.`when`(appDataManager.register(bodyRegister))
                    .thenReturn(Single.error(errorExpected))
            }

            When("request api register") {
                viewModel.register(bodyRegister)
            }

            Then("result Bad Request") {
                verify(observerRegister, atLeastOnce()).onChanged(ResultState.Loading)
                viewModel.resultStateResponseRegister.test().assertValue {
                    it is ResultState.Error
                }
                viewModel.resultStateResponseRegister.hasActiveObservers()
            }
        }

        Scenario("do on insert to db room, response any value") {
            Given("set values") {
                given(appDataManager.insertUser(User())).willReturn(Observable.just(true))
            }

            When("request dao ") {
                viewModel.insertUser(User())
            }

            Then("result true and get values") {
                verify(appDataManager).insertUser(User())
                viewModel.resultStateInserDb.test().assertHasValue()
                viewModel.resultStateInserDb.test().assertValue {
                    it is ResultState.Success<*>
                }
            }
        }

        afterFeature {
            TrampolineSchedulerRX.tearDown()
            InstantRuleExecution.tearDown()
        }
    }
})