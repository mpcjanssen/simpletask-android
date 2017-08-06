package nl.mpcjanssen.simpletask.remote

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.content.Context
import android.os.Bundle
import android.accounts.NetworkErrorException
import android.accounts.AccountManager
import android.content.Intent



class SimpleTaskNextcloudAuthenticator constructor(context: Context): AbstractAccountAuthenticator(context) {

    var mContext : Context = context

    override fun editProperties(response: AccountAuthenticatorResponse,
                                accountType: String): Bundle? {
        // TODO Auto-generated method stub
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun addAccount(response: AccountAuthenticatorResponse,
                            accountType: String, authTokenType: String,
                            requiredFeatures: Array<String>, options: Bundle): Bundle {
        val result: Bundle
        val intent: Intent

        intent = Intent(mContext, LoginScreen::class.java)

        result = Bundle()
        result.putParcelable(AccountManager.KEY_INTENT, intent)
        return result
    }

    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(response: AccountAuthenticatorResponse,
                                    account: Account, options: Bundle): Bundle? {
        // TODO Auto-generated method stub
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(response: AccountAuthenticatorResponse,
                              account: Account, authTokenType: String, options: Bundle): Bundle? {
        // TODO Auto-generated method stub
        return null
    }

    override fun getAuthTokenLabel(authTokenType: String): String? {
        // TODO Auto-generated method stub
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun updateCredentials(response: AccountAuthenticatorResponse,
                                   account: Account, authTokenType: String, options: Bundle): Bundle? {
        // TODO Auto-generated method stub
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun hasFeatures(response: AccountAuthenticatorResponse,
                             account: Account, features: Array<String>): Bundle? {
        // TODO Auto-generated method stub
        return null
    }

}