-- ========================================
-- VERIFY CHAT CLEANUP
-- Check if all chat data has been deleted
-- ========================================

USE WorkFlowManagerDB;

PRINT '=== CHAT CLEANUP VERIFICATION ==='
PRINT ''

-- Check chat_messages table
DECLARE @messageCount INT
SELECT @messageCount = COUNT(*) FROM chat_messages
PRINT 'Chat Messages: ' + CAST(@messageCount AS VARCHAR(10))

-- Check chat_files table  
DECLARE @fileCount INT
SELECT @fileCount = COUNT(*) FROM chat_files
PRINT 'Chat Files: ' + CAST(@fileCount AS VARCHAR(10))

-- Check if tables exist
IF OBJECT_ID('chat_messages', 'U') IS NOT NULL
    PRINT 'chat_messages table: EXISTS'
ELSE
    PRINT 'chat_messages table: NOT FOUND'

IF OBJECT_ID('chat_files', 'U') IS NOT NULL
    PRINT 'chat_files table: EXISTS'
ELSE
    PRINT 'chat_files table: NOT FOUND'

-- Summary
PRINT ''
IF @messageCount = 0 AND @fileCount = 0
    PRINT '✅ SUCCESS: All chat data has been deleted!'
ELSE
    PRINT '⚠️ WARNING: Some chat data still remains'

PRINT ''
PRINT '=== VERIFICATION COMPLETED ==='
