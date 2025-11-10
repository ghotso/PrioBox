package com.vipmail.ui.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vipmail.data.model.EmailMessage
import com.vipmail.data.repository.MailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageDetailViewModel @Inject constructor(
    private val mailRepository: MailRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class State(
        val isLoading: Boolean = true,
        val message: EmailMessage? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val messageId: Long? = savedStateHandle["messageId"]
    private var hasMarkedRead = false

    init {
        if (messageId == null) {
            _state.value = State(isLoading = false, error = "Message not found")
        } else {
            mailRepository.observeMessage(messageId)
                .onEach { message ->
                    if (message == null) {
                        _state.value = State(isLoading = false, error = "Message no longer exists")
                    } else {
                        _state.value = State(isLoading = false, message = message)
                        markReadOnce(message.id)
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun markReadOnce(id: Long) {
        if (hasMarkedRead) return
        hasMarkedRead = true
        viewModelScope.launch {
            mailRepository.markMessageRead(id)
        }
    }
}

