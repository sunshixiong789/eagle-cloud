package com.eagle.seata.tcc;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * {@link TccIdempotencyHelper} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TccIdempotencyHelper")
class TccIdempotencyHelperTest {

    private static final String XID_1 = "192.168.1.1:8091:12345";
    private static final String XID_2 = "192.168.1.1:8091:99999";
    private static final long BRANCH_ID_1 = 100L;
    private static final long BRANCH_ID_2 = 200L;

    @Mock
    private BusinessActionContext ctx1;

    @Mock
    private BusinessActionContext ctx2;

    private TccIdempotencyHelper helper;

    @BeforeEach
    void setUp() {
        helper = new TccIdempotencyHelper();
        when(ctx1.getXid()).thenReturn(XID_1);
        when(ctx1.getBranchId()).thenReturn(BRANCH_ID_1);
        when(ctx2.getXid()).thenReturn(XID_2);
        when(ctx2.getBranchId()).thenReturn(BRANCH_ID_2);
    }

    @Nested
    @DisplayName("isConfirmed / markConfirmed")
    class Confirmed {

        @Test
        @DisplayName("should return false on first confirm check before marking")
        void shouldReturnFalseOnFirstConfirm() {
            assertFalse(helper.isConfirmed(ctx1),
                    "isConfirmed should be false before markConfirmed is called");
        }

        @Test
        @DisplayName("should return true on duplicate confirm after marking")
        void shouldReturnTrueOnDuplicateConfirm() {
            helper.markConfirmed(ctx1);

            assertTrue(helper.isConfirmed(ctx1),
                    "isConfirmed should be true after markConfirmed is called");
        }

        @Test
        @DisplayName("should not affect another branch when marking one confirmed")
        void shouldNotAffectOtherBranchWhenConfirmed() {
            helper.markConfirmed(ctx1);

            assertFalse(helper.isConfirmed(ctx2),
                    "ctx2 should remain unconfirmed after ctx1 is confirmed");
        }
    }

    @Nested
    @DisplayName("isCancelled / markCancelled")
    class Cancelled {

        @Test
        @DisplayName("should return false on first cancel check before marking")
        void shouldReturnFalseOnFirstCancel() {
            assertFalse(helper.isCancelled(ctx1),
                    "isCancelled should be false before markCancelled is called");
        }

        @Test
        @DisplayName("should return true on duplicate cancel after marking")
        void shouldReturnTrueOnDuplicateCancel() {
            helper.markCancelled(ctx1);

            assertTrue(helper.isCancelled(ctx1),
                    "isCancelled should be true after markCancelled is called");
        }

        @Test
        @DisplayName("should not affect another branch when marking one cancelled")
        void shouldNotAffectOtherBranchWhenCancelled() {
            helper.markCancelled(ctx1);

            assertFalse(helper.isCancelled(ctx2),
                    "ctx2 should remain uncancelled after ctx1 is cancelled");
        }
    }

    @Nested
    @DisplayName("confirm and cancel isolation")
    class ConfirmAndCancelIsolation {

        @Test
        @DisplayName("should isolate confirm and cancel states for the same branch")
        void shouldIsolateConfirmAndCancel() {
            // Mark ctx1 as confirmed — cancel should still be false
            helper.markConfirmed(ctx1);

            assertTrue(helper.isConfirmed(ctx1),
                    "ctx1 should be confirmed");
            assertFalse(helper.isCancelled(ctx1),
                    "ctx1 should not be cancelled when only confirmed");
        }

        @Test
        @DisplayName("should isolate cancel from confirm state for the same branch")
        void shouldIsolateCancelFromConfirm() {
            // Mark ctx1 as cancelled — confirm should still be false
            helper.markCancelled(ctx1);

            assertTrue(helper.isCancelled(ctx1),
                    "ctx1 should be cancelled");
            assertFalse(helper.isConfirmed(ctx1),
                    "ctx1 should not be confirmed when only cancelled");
        }

        @Test
        @DisplayName("should allow independent states for two different branches")
        void shouldAllowIndependentStatesForDifferentBranches() {
            helper.markConfirmed(ctx1);
            helper.markCancelled(ctx2);

            assertTrue(helper.isConfirmed(ctx1),
                    "ctx1 should be confirmed");
            assertFalse(helper.isCancelled(ctx1),
                    "ctx1 should not be cancelled");
            assertTrue(helper.isCancelled(ctx2),
                    "ctx2 should be cancelled");
            assertFalse(helper.isConfirmed(ctx2),
                    "ctx2 should not be confirmed");
        }

        @Test
        @DisplayName("should overwrite confirm state with cancel when same branch is marked cancelled after confirmed")
        void shouldOverwriteConfirmWithCancel() {
            helper.markConfirmed(ctx1);
            helper.markCancelled(ctx1);

            // markCancelled overwrites the key, so confirm is no longer true
            assertFalse(helper.isConfirmed(ctx1),
                    "After markCancelled overwrites the state, isConfirmed should be false");
            assertTrue(helper.isCancelled(ctx1),
                    "After markCancelled, isCancelled should be true");
        }
    }

    @Nested
    @DisplayName("same xid different branchId")
    class SameXidDifferentBranch {

        @Test
        @DisplayName("should treat same xid with different branchId as separate keys")
        void shouldTreatSameXidDifferentBranchAsSeparate() {
            // ctx1 has XID_1:BRANCH_ID_1; build a third context with same XID but different branchId
            BusinessActionContext ctx3 = org.mockito.Mockito.mock(BusinessActionContext.class);
            when(ctx3.getXid()).thenReturn(XID_1);
            when(ctx3.getBranchId()).thenReturn(BRANCH_ID_2);

            helper.markConfirmed(ctx1);

            assertFalse(helper.isConfirmed(ctx3),
                    "Different branchId under same XID should be treated as a separate key");
        }
    }
}
