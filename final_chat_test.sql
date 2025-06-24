-- ========================================
-- FINAL CHAT SYSTEM TEST
-- Comprehensive test after all fixes
-- ========================================

USE WorkFlowManagerDB;

PRINT '=== FINAL CHAT SYSTEM TEST ==='
PRINT ''

-- Test 1: Current Data State
PRINT '--- TEST 1: CURRENT DATA STATE ---'
SELECT 
    message_id,
    sender_id,
    sender_name,
    sender_role,
    receiver_id,
    receiver_name,
    CASE 
        WHEN LEN(CAST(message AS NVARCHAR(MAX))) > 40 
        THEN SUBSTRING(CAST(message AS NVARCHAR(MAX)), 1, 40) + '...'
        ELSE CAST(message AS NVARCHAR(MAX))
    END as message_preview,
    FORMAT(timestamp, 'yyyy-MM-dd HH:mm:ss') as formatted_timestamp,
    is_read,
    has_attachments
FROM chat_messages 
ORDER BY message_id;

PRINT ''
PRINT '--- FILES ---'
SELECT 
    file_id,
    message_id,
    file_name,
    original_file_name,
    file_size,
    uploaded_by
FROM chat_files 
ORDER BY file_id;

-- Test 2: Data Integrity Checks
PRINT ''
PRINT '--- TEST 2: DATA INTEGRITY CHECKS ---'

-- Check for duplicates
PRINT 'Checking for duplicate messages...'
SELECT 
    COUNT(*) as duplicate_count,
    'Duplicate messages found' as check_type
FROM (
    SELECT sender_id, receiver_id, CAST(message AS NVARCHAR(MAX)) as message_text, COUNT(*) as count
    FROM chat_messages 
    GROUP BY sender_id, receiver_id, CAST(message AS NVARCHAR(MAX))
    HAVING COUNT(*) > 1
) duplicates;

-- Check receiver_name consistency
PRINT 'Checking receiver_name consistency...'
SELECT 
    COUNT(*) as null_receiver_count,
    'Messages with NULL receiver_name' as check_type
FROM chat_messages 
WHERE receiver_name IS NULL;

-- Check attachment consistency
PRINT 'Checking attachment flag consistency...'
SELECT 
    COUNT(*) as inconsistent_count,
    'Messages with inconsistent attachment flags' as check_type
FROM (
    SELECT cm.message_id
    FROM chat_messages cm
    LEFT JOIN chat_files cf ON cm.message_id = cf.message_id
    GROUP BY cm.message_id, cm.has_attachments
    HAVING (cm.has_attachments = 1 AND COUNT(cf.file_id) = 0) 
        OR (cm.has_attachments = 0 AND COUNT(cf.file_id) > 0)
) inconsistent;

-- Test 3: Relationship Integrity
PRINT ''
PRINT '--- TEST 3: RELATIONSHIP INTEGRITY ---'

-- Check orphaned files
PRINT 'Checking for orphaned files...'
SELECT 
    COUNT(*) as orphan_count,
    'Orphaned files (no matching message)' as check_type
FROM chat_files cf
LEFT JOIN chat_messages cm ON cf.message_id = cm.message_id
WHERE cm.message_id IS NULL;

-- Check invalid user references
PRINT 'Checking for invalid user references...'
SELECT 
    COUNT(*) as invalid_sender_count,
    'Messages with invalid sender_id' as check_type
FROM chat_messages cm
LEFT JOIN users u ON cm.sender_id = u.user_id
WHERE u.user_id IS NULL;

SELECT 
    COUNT(*) as invalid_receiver_count,
    'Messages with invalid receiver_id' as check_type
FROM chat_messages cm
LEFT JOIN users u ON cm.receiver_id = u.user_id
WHERE u.user_id IS NULL;

-- Test 4: Business Logic Validation
PRINT ''
PRINT '--- TEST 4: BUSINESS LOGIC VALIDATION ---'

-- Check for self-messages (should not exist)
PRINT 'Checking for self-messages...'
SELECT 
    COUNT(*) as self_message_count,
    'Self-messages (sender = receiver)' as check_type
FROM chat_messages 
WHERE sender_id = receiver_id;

-- Check role consistency
PRINT 'Checking role consistency...'
SELECT 
    cm.sender_role,
    u.role as actual_role,
    COUNT(*) as count,
    CASE 
        WHEN cm.sender_role = u.role THEN 'CONSISTENT'
        ELSE 'INCONSISTENT'
    END as status
FROM chat_messages cm
INNER JOIN users u ON cm.sender_id = u.user_id
GROUP BY cm.sender_role, u.role
ORDER BY cm.sender_role, u.role;

-- Test 5: Performance and Statistics
PRINT ''
PRINT '--- TEST 5: STATISTICS ---'

DECLARE @totalMessages INT, @totalFiles INT, @totalFileSize BIGINT
DECLARE @adminMessages INT, @userMessages INT
DECLARE @readMessages INT, @unreadMessages INT

SELECT @totalMessages = COUNT(*) FROM chat_messages;
SELECT @totalFiles = COUNT(*) FROM chat_files;
SELECT @totalFileSize = ISNULL(SUM(file_size), 0) FROM chat_files;
SELECT @adminMessages = COUNT(*) FROM chat_messages WHERE sender_role = 'ADMIN';
SELECT @userMessages = COUNT(*) FROM chat_messages WHERE sender_role = 'USER';
SELECT @readMessages = COUNT(*) FROM chat_messages WHERE is_read = 1;
SELECT @unreadMessages = COUNT(*) FROM chat_messages WHERE is_read = 0;

PRINT 'Total Messages: ' + CAST(@totalMessages AS VARCHAR(10))
PRINT 'Total Files: ' + CAST(@totalFiles AS VARCHAR(10))
PRINT 'Total File Size: ' + CAST(@totalFileSize AS VARCHAR(20)) + ' bytes'
PRINT 'Admin Messages: ' + CAST(@adminMessages AS VARCHAR(10))
PRINT 'User Messages: ' + CAST(@userMessages AS VARCHAR(10))
PRINT 'Read Messages: ' + CAST(@readMessages AS VARCHAR(10))
PRINT 'Unread Messages: ' + CAST(@unreadMessages AS VARCHAR(10))

-- Final Assessment
PRINT ''
PRINT '--- FINAL ASSESSMENT ---'

DECLARE @issues INT = 0

-- Count all potential issues
SELECT @issues = @issues + COUNT(*) FROM (
    SELECT sender_id, receiver_id, CAST(message AS NVARCHAR(MAX)) as message_text, COUNT(*) as count
    FROM chat_messages 
    GROUP BY sender_id, receiver_id, CAST(message AS NVARCHAR(MAX))
    HAVING COUNT(*) > 1
) duplicates;

SELECT @issues = @issues + COUNT(*) FROM chat_messages WHERE receiver_name IS NULL;

SELECT @issues = @issues + COUNT(*) FROM (
    SELECT cm.message_id
    FROM chat_messages cm
    LEFT JOIN chat_files cf ON cm.message_id = cf.message_id
    GROUP BY cm.message_id, cm.has_attachments
    HAVING (cm.has_attachments = 1 AND COUNT(cf.file_id) = 0) 
        OR (cm.has_attachments = 0 AND COUNT(cf.file_id) > 0)
) inconsistent;

SELECT @issues = @issues + COUNT(*) FROM chat_messages WHERE sender_id = receiver_id;

PRINT 'Total Issues Found: ' + CAST(@issues AS VARCHAR(10))

IF @issues = 0
    PRINT '✅ ALL TESTS PASSED - Chat system is working correctly!'
ELSE
    PRINT '⚠️ ISSUES DETECTED - Please review the test results above'

PRINT ''
PRINT '=== TEST COMPLETED ==='
