package com.lumbridgeguide.account;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AccountStatusTextTest {

    @Test
    public void mapsTheIronmanVarbitToTheBackendAccountType() {
        assertEquals("REGULAR", AccountStatusText.accountType(0));
        assertEquals("IRONMAN", AccountStatusText.accountType(1));
        assertEquals("ULTIMATE_IRONMAN", AccountStatusText.accountType(2));
        assertEquals("HARDCORE_IRONMAN", AccountStatusText.accountType(3));
        assertEquals("GROUP_IRONMAN", AccountStatusText.accountType(4));
        assertEquals("HARDCORE_GROUP_IRONMAN", AccountStatusText.accountType(5));
        assertEquals("UNRANKED_GROUP_IRONMAN", AccountStatusText.accountType(6));
    }

    @Test
    public void linkErrorsUpdateTheStatusButOtherErrorsDoNot() {
        assertEquals("LINKED_ELSEWHERE", AccountStatusText.statusForError("ACCOUNT_LINKED_ELSEWHERE"));
        assertEquals("LIMIT_REACHED", AccountStatusText.statusForError("ACCOUNT_LIMIT_REACHED"));
        assertEquals("LOCKED", AccountStatusText.statusForError("ACCOUNT_LOCKED"));
        assertEquals("NOT_LINKED", AccountStatusText.statusForError("ACCOUNT_NOT_LINKED"));
        assertNull(AccountStatusText.statusForError("RATE_LIMITED"));
        assertNull(AccountStatusText.statusForError("WORLD_NOT_SUPPORTED"));
        assertNull(AccountStatusText.statusForError(null));
    }

    @Test
    public void wordsTheAccountTypeAndBadgeLikeTheWebsite() {
        assertEquals("Main", AccountStatusText.accountTypeLabel("REGULAR"));
        assertEquals("Hardcore group ironman", AccountStatusText.accountTypeLabel("HARDCORE_GROUP_IRONMAN"));
        assertEquals("", AccountStatusText.accountTypeLabel(null));
        assertEquals("LINKED ELSEWHERE", AccountStatusText.statusBadge("LINKED_ELSEWHERE"));
        assertEquals("", AccountStatusText.statusBadge(null));
    }

    @Test
    public void warnsWhenTheAccountCannotSync() {
        assertEquals(AccountStatusText.Tone.SUCCESS, AccountStatusText.forStatus("LINKED").getTone());
        assertEquals(AccountStatusText.Tone.WARNING, AccountStatusText.forStatus("LIMIT_REACHED").getTone());
        assertEquals(AccountStatusText.Tone.WARNING, AccountStatusText.forStatus("LOCKED").getTone());
        assertEquals(AccountStatusText.Tone.ERROR, AccountStatusText.forStatus("LINKED_ELSEWHERE").getTone());
    }
}
