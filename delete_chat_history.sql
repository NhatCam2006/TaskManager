-- ========================================
-- DELETE ALL CHAT HISTORY
-- WARNING: This will permanently delete all chat messages and files!
-- ========================================

USE WorkFlowManagerDB;

-- Show current statistics before deletion
SELECT 'BEFORE DELETION - Messages:' as Info, COUNT(*) as Count FROM chat_messages
UNION ALL
SELECT 'BEFORE DELETION - Files:', COUNT(*) FROM chat_files;

-- Start transaction
BEGIN TRANSACTION

BEGIN TRY
    -- Step 1: Delete from chat_files table (must delete first due to foreign key)
    DELETE FROM chat_files;
    PRINT 'Deleted all records from chat_files table'

    -- Step 2: Delete from chat_messages table
    DELETE FROM chat_messages;
    PRINT 'Deleted all records from chat_messages table'

    -- Step 3: Reset identity columns (restart IDs from 1)
    DBCC CHECKIDENT ('chat_messages', RESEED, 0);
    DBCC CHECKIDENT ('chat_files', RESEED, 0);
    PRINT 'Reset identity columns for both tables'

    -- Commit transaction
    COMMIT TRANSACTION
    PRINT 'Database cleanup completed successfully'

END TRY
BEGIN CATCH
    -- Rollback transaction on error
    ROLLBACK TRANSACTION
    PRINT 'Error occurred during deletion'
    PRINT ERROR_MESSAGE()
END CATCH

-- Show final statistics
SELECT 'AFTER DELETION - Messages:' as Info, COUNT(*) as Count FROM chat_messages
UNION ALL
SELECT 'AFTER DELETION - Files:', COUNT(*) FROM chat_files;
