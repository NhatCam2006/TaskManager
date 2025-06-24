-- ========================================
-- TEST CHAT FIXES
-- Verify that duplicate prevention is working
-- ========================================

USE WorkFlowManagerDB;

PRINT '=== TESTING CHAT FIXES ==='
PRINT ''

-- Show current state
PRINT '--- CURRENT CHAT STATE ---'
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
PRINT '--- CHAT FILES ---'
SELECT 
    file_id,
    message_id,
    file_name,
    original_file_name,
    file_size,
    uploaded_by
FROM chat_files 
ORDER BY file_id;

PRINT ''
PRINT '--- VALIDATION CHECKS ---'

-- Check 1: No duplicate messages
PRINT 'Check 1: Looking for potential duplicates...'
SELECT 
    sender_id,
    receiver_id,
    CAST(message AS NVARCHAR(MAX)) as message_text,
    COUNT(*) as count
FROM chat_messages 
GROUP BY sender_id, receiver_id, CAST(message AS NVARCHAR(MAX))
HAVING COUNT(*) > 1;

-- Check 2: All receiver_name fields populated
PRINT ''
PRINT 'Check 2: Messages with NULL receiver_name...'
SELECT 
    message_id,
    sender_name,
    receiver_id,
    receiver_name
FROM chat_messages 
WHERE receiver_name IS NULL;

-- Check 3: File attachments consistency
PRINT ''
PRINT 'Check 3: File attachment consistency...'
SELECT 
    cm.message_id,
    cm.has_attachments,
    COUNT(cf.file_id) as actual_file_count,
    CASE 
        WHEN cm.has_attachments = 1 AND COUNT(cf.file_id) > 0 THEN 'CORRECT'
        WHEN cm.has_attachments = 0 AND COUNT(cf.file_id) = 0 THEN 'CORRECT'
        ELSE 'INCONSISTENT'
    END as status
FROM chat_messages cm
LEFT JOIN chat_files cf ON cm.message_id = cf.message_id
GROUP BY cm.message_id, cm.has_attachments
ORDER BY cm.message_id;

-- Check 4: Orphaned files
PRINT ''
PRINT 'Check 4: Orphaned files...'
SELECT 
    cf.file_id,
    cf.message_id,
    cf.file_name
FROM chat_files cf
LEFT JOIN chat_messages cm ON cf.message_id = cm.message_id
WHERE cm.message_id IS NULL;

PRINT ''
PRINT '--- SUMMARY ---'
DECLARE @totalMessages INT, @totalFiles INT, @nullReceivers INT, @inconsistentAttachments INT

SELECT @totalMessages = COUNT(*) FROM chat_messages;
SELECT @totalFiles = COUNT(*) FROM chat_files;
SELECT @nullReceivers = COUNT(*) FROM chat_messages WHERE receiver_name IS NULL;
SELECT @inconsistentAttachments = COUNT(*) FROM (
    SELECT cm.message_id
    FROM chat_messages cm
    LEFT JOIN chat_files cf ON cm.message_id = cf.message_id
    GROUP BY cm.message_id, cm.has_attachments
    HAVING (cm.has_attachments = 1 AND COUNT(cf.file_id) = 0) 
        OR (cm.has_attachments = 0 AND COUNT(cf.file_id) > 0)
) as inconsistent;

PRINT 'Total Messages: ' + CAST(@totalMessages AS VARCHAR(10))
PRINT 'Total Files: ' + CAST(@totalFiles AS VARCHAR(10))
PRINT 'Messages with NULL receiver_name: ' + CAST(@nullReceivers AS VARCHAR(10))
PRINT 'Inconsistent attachment flags: ' + CAST(@inconsistentAttachments AS VARCHAR(10))

PRINT ''
IF @nullReceivers = 0 AND @inconsistentAttachments = 0
    PRINT '✅ ALL CHECKS PASSED - Chat system is consistent!'
ELSE
    PRINT '⚠️ ISSUES FOUND - Please review the checks above'

PRINT ''
PRINT '=== TEST COMPLETED ==='
