package com.example.resq.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.example.resq.data.UserSessionManager
import com.example.resq.ui.model.Community
import com.example.resq.ui.model.EmergencyContact
import com.example.resq.ui.navigation.NavigationHost
import com.example.resq.ui.screen.companion.CompanionScreen
import com.example.resq.ui.theme.ResQTheme

/**
 * Fragment hosting the Companion feature:
 * contacts list, communities, add contact, create community, select members.
 * All data is persisted via UserSessionManager (SharedPreferences).
 */
class CompanionFragment : Fragment() {

    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = UserSessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val navHost = activity as? NavigationHost

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ResQTheme {
                    // Mutable state so UI recomposes on add/update
                    var contacts    by remember { mutableStateOf(sessionManager.getEmergencyContacts()) }
                    var communities by remember { mutableStateOf(sessionManager.getCommunities()) }

                    CompanionScreen(
                        contacts     = contacts,
                        communities  = communities,

                        onAddContact = { name, relationship, phone ->
                            val nextId = (contacts.maxOfOrNull { it.id } ?: 0) + 1
                            sessionManager.addEmergencyContact(
                                EmergencyContact(
                                    id           = nextId,
                                    name         = name,
                                    relationship = relationship,
                                    phone        = phone,
                                )
                            )
                            contacts = sessionManager.getEmergencyContacts()
                        },

                        onCreateCommunity = { name, memberIds ->
                            val nextId = (communities.maxOfOrNull { it.id } ?: 0) + 1
                            sessionManager.addCommunity(
                                Community(id = nextId, name = name, memberIds = memberIds)
                            )
                            communities = sessionManager.getCommunities()
                        },

                        onUpdateCommunityMembers = { communityId, memberIds ->
                            sessionManager.updateCommunityMembers(communityId, memberIds)
                            communities = sessionManager.getCommunities()
                        },

                        onNavItemSelected = { route ->
                            when (route) {
                                "home"    -> navHost?.navigateToHome()
                                "map"     -> navHost?.navigateToMap()
                                "report"  -> navHost?.navigateToReport()
                                "profile" -> navHost?.navigateToProfile()
                            }
                        },

                        onFabClick = { /* already on companion */ },
                    )
                }
            }
        }
    }
}
