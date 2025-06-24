-- ========================================
-- CHECK CHAT DATA AND ANALYZE ISSUES
-- ========================================

USE WorkFlowManagerDB;

PRINT '=== CHAT DATA ANALYSIS ==='
PRINT ''

-- Check users table
PRINT '--- USERS ---'
SELECT user_id, username, full_name, role, is_active 
FROM users 
ORDER BY user_id;

PRINT ''
PRINT '--- CHAT MESSAGES ---'
-- Check chat messages with detailed info
SELECT
    message_id,
    sender_id,
    sender_name,
    sender_role,
    receiver_id,
    receiver_name,
    CASE
        WHEN LEN(CAST(message AS NVARCHAR(MAX))) > 50
        THEN SUBSTRING(CAST(message AS NVARCHAR(MAX)), 1, 50) + '...'
        ELSE CAST(message AS NVARCHAR(MAX))
    END as message_preview,
    timestamp,
    is_read,
    has_attachments
FROM chat_messages
ORDER BY message_id;

PRINT ''
PRINT '--- CHAT FILES ---'
-- Check chat files
SELECT 
    file_id,
    message_id,
    file_name,
    original_file_name,
    file_type,
    file_size,
    uploaded_at,
    uploaded_by
FROM chat_files 
ORDER BY file_id;

PRINT ''
PRINT '--- POTENTIAL ISSUES ---'

-- Check for messages with has_attachments = false but have files
SELECT
    cm.message_id,
    CASE
        WHEN LEN(CAST(cm.message AS NVARCHAR(MAX))) > 30
        THEN SUBSTRING(CAST(cm.message AS NVARCHAR(MAX)), 1, 30) + '...'
        ELSE CAST(cm.message AS NVARCHAR(MAX))
    END as message_preview,
    cm.has_attachments,
    COUNT(cf.file_id) as actual_file_count
FROM chat_messages cm
LEFT JOIN chat_files cf ON cm.message_id = cf.message_id
GROUP BY cm.message_id, CAST(cm.message AS NVARCHAR(MAX)), cm.has_attachments
HAVING (cm.has_attachments = 0 AND COUNT(cf.file_id) > 0)
    OR (cm.has_attachments = 1 AND COUNT(cf.file_id) = 0);

PRINT ''
PRINT '--- SUMMARY ---'
DECLARE @totalMessages INT, @totalFiles INT, @messagesWithFiles INT, @orphanFiles INT

SELECT @totalMessages = COUNT(*) FROM chat_messages;
SELECT @totalFiles = COUNT(*) FROM chat_files;
SELECT @messagesWithFiles = COUNT(*) FROM chat_messages WHERE has_attachments = 1;
SELECT @orphanFiles = COUNT(*) FROM chat_files cf 
WHERE NOT EXISTS (SELECT 1 FROM chat_messages cm WHERE cm.message_id = cf.message_id);

PRINT 'Total Messages: ' + CAST(@totalMessages AS VARCHAR(10))
PRINT 'Total Files: ' + CAST(@totalFiles AS VARCHAR(10))
PRINT 'Messages marked with attachments: ' + CAST(@messagesWithFiles AS VARCHAR(10))
PRINT 'Orphan files (no matching message): ' + CAST(@orphanFiles AS VARCHAR(10))

PRINT ''
PRINT '=== ANALYSIS COMPLETED ==='
