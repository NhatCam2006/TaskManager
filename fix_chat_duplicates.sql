-- ========================================
-- FIX CHAT MESSAGE DUPLICATES
-- Carefully remove duplicate messages while preserving data integrity
-- ========================================

USE WorkFlowManagerDB;

PRINT '=== CHAT DUPLICATE FIX ANALYSIS ==='
PRINT ''

-- Show current state before fix
PRINT '--- BEFORE FIX ---'
SELECT 
    message_id,
    sender_id,
    sender_name,
    sender_role,
    receiver_id,
    receiver_name,
    CASE 
        WHEN LEN(CAST(message AS NVARCHAR(MAX))) > 30 
        THEN SUBSTRING(CAST(message AS NVARCHAR(MAX)), 1, 30) + '...'
        ELSE CAST(message AS NVARCHAR(MAX))
    END as message_preview,
    timestamp,
    is_read,
    has_attachments
FROM chat_messages 
ORDER BY message_id;

PRINT ''
PRINT '--- FILES BEFORE FIX ---'
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
PRINT '--- IDENTIFYING DUPLICATES ---'

-- Identify problematic messages
-- Message 4: admin -> admin (invalid self-message)
-- Message 5: duplicate of message 3 content

DECLARE @message4_exists INT = 0
DECLARE @message5_exists INT = 0

SELECT @message4_exists = COUNT(*) FROM chat_messages WHERE message_id = 4
SELECT @message5_exists = COUNT(*) FROM chat_messages WHERE message_id = 5

PRINT 'Message 4 exists: ' + CAST(@message4_exists AS VARCHAR(10))
PRINT 'Message 5 exists: ' + CAST(@message5_exists AS VARCHAR(10))

-- Start transaction for safe cleanup
BEGIN TRANSACTION

BEGIN TRY
    PRINT ''
    PRINT '--- STARTING CLEANUP ---'
    
    -- Step 1: Remove invalid self-message (message_id = 4)
    IF @message4_exists > 0
    BEGIN
        DELETE FROM chat_messages WHERE message_id = 4
        PRINT 'Deleted invalid self-message (ID: 4)'
    END
    
    -- Step 2: Remove duplicate message (message_id = 5)
    IF @message5_exists > 0
    BEGIN
        DELETE FROM chat_messages WHERE message_id = 5
        PRINT 'Deleted duplicate message (ID: 5)'
    END
    
    -- Step 3: Verify file attachments are still properly linked
    PRINT ''
    PRINT '--- VERIFYING FILE ATTACHMENTS ---'
    
    -- Check if any files are orphaned
    DECLARE @orphan_files INT
    SELECT @orphan_files = COUNT(*) 
    FROM chat_files cf 
    WHERE NOT EXISTS (SELECT 1 FROM chat_messages cm WHERE cm.message_id = cf.message_id)
    
    PRINT 'Orphaned files after cleanup: ' + CAST(@orphan_files AS VARCHAR(10))
    
    -- Step 4: Fix receiver_name NULL values
    PRINT ''
    PRINT '--- FIXING NULL RECEIVER NAMES ---'
    
    -- Update NULL receiver names for messages to admin
    UPDATE cm 
    SET receiver_name = u.full_name
    FROM chat_messages cm
    INNER JOIN users u ON cm.receiver_id = u.user_id
    WHERE cm.receiver_name IS NULL
    
    DECLARE @fixed_names INT = @@ROWCOUNT
    PRINT 'Fixed NULL receiver names: ' + CAST(@fixed_names AS VARCHAR(10))
    
    -- Commit transaction
    COMMIT TRANSACTION
    PRINT ''
    PRINT '✅ CLEANUP COMPLETED SUCCESSFULLY'
    
END TRY
BEGIN CATCH
    -- Rollback on error
    ROLLBACK TRANSACTION
    PRINT ''
    PRINT '❌ ERROR DURING CLEANUP'
    PRINT 'Error: ' + ERROR_MESSAGE()
END CATCH

PRINT ''
PRINT '--- AFTER FIX ---'
SELECT 
    message_id,
    sender_id,
    sender_name,
    sender_role,
    receiver_id,
    receiver_name,
    CASE 
        WHEN LEN(CAST(message AS NVARCHAR(MAX))) > 30 
        THEN SUBSTRING(CAST(message AS NVARCHAR(MAX)), 1, 30) + '...'
        ELSE CAST(message AS NVARCHAR(MAX))
    END as message_preview,
    timestamp,
    is_read,
    has_attachments
FROM chat_messages 
ORDER BY message_id;

PRINT ''
PRINT '--- FILES AFTER FIX ---'
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
PRINT '--- FINAL SUMMARY ---'
DECLARE @final_messages INT, @final_files INT
SELECT @final_messages = COUNT(*) FROM chat_messages
SELECT @final_files = COUNT(*) FROM chat_files

PRINT 'Total messages after fix: ' + CAST(@final_messages AS VARCHAR(10))
PRINT 'Total files after fix: ' + CAST(@final_files AS VARCHAR(10))

PRINT ''
PRINT '=== FIX COMPLETED ==='
