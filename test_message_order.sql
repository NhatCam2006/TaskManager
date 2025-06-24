-- ========================================
-- TEST MESSAGE ORDER
-- Verify message display order is correct
-- ========================================

USE WorkFlowManagerDB;

PRINT '=== TESTING MESSAGE ORDER ==='
PRINT ''

-- Show messages in the order they should appear in UI (chronological)
PRINT '--- MESSAGES IN CHRONOLOGICAL ORDER (as they should appear in UI) ---'
PRINT 'Format: [Time] Sender -> Receiver: Message'
PRINT ''

SELECT 
    FORMAT(timestamp, 'HH:mm:ss') as time_only,
    sender_name + ' (' + CAST(sender_id AS VARCHAR) + ')' as sender_info,
    receiver_name + ' (' + CAST(receiver_id AS VARCHAR) + ')' as receiver_info,
    CASE 
        WHEN LEN(CAST(message AS NVARCHAR(MAX))) > 40 
        THEN SUBSTRING(CAST(message AS NVARCHAR(MAX)), 1, 40) + '...'
        ELSE CAST(message AS NVARCHAR(MAX))
    END as message_preview,
    CASE 
        WHEN has_attachments = 1 THEN '📎'
        ELSE ''
    END as attachment_icon
FROM chat_messages 
ORDER BY timestamp ASC;

PRINT ''
PRINT '--- EXPECTED UI DISPLAY ORDER ---'
PRINT '(Oldest messages at top, newest at bottom)'
PRINT ''

DECLARE @counter INT = 1
DECLARE @time VARCHAR(10), @sender VARCHAR(100), @receiver VARCHAR(100), @msg VARCHAR(100), @attach VARCHAR(5)

DECLARE message_cursor CURSOR FOR
SELECT 
    FORMAT(timestamp, 'HH:mm:ss'),
    sender_name,
    receiver_name,
    CASE 
        WHEN LEN(CAST(message AS NVARCHAR(MAX))) > 30 
        THEN SUBSTRING(CAST(message AS NVARCHAR(MAX)), 1, 30) + '...'
        ELSE CAST(message AS NVARCHAR(MAX))
    END,
    CASE WHEN has_attachments = 1 THEN '📎' ELSE '' END
FROM chat_messages 
ORDER BY timestamp ASC

OPEN message_cursor
FETCH NEXT FROM message_cursor INTO @time, @sender, @receiver, @msg, @attach

WHILE @@FETCH_STATUS = 0
BEGIN
    PRINT CAST(@counter AS VARCHAR) + '. [' + @time + '] ' + @sender + ' -> ' + @receiver + ': ' + @msg + ' ' + @attach
    SET @counter = @counter + 1
    FETCH NEXT FROM message_cursor INTO @time, @sender, @receiver, @msg, @attach
END

CLOSE message_cursor
DEALLOCATE message_cursor

PRINT ''
PRINT '--- CONVERSATION FLOW ANALYSIS ---'

-- Analyze conversation flow between admin and user
PRINT 'Conversation between Admin and dinh nhat cam:'
PRINT ''

SELECT 
    ROW_NUMBER() OVER (ORDER BY timestamp ASC) as step,
    FORMAT(timestamp, 'HH:mm:ss') as time_only,
    CASE 
        WHEN sender_id = 1 THEN 'Admin -> User'
        WHEN sender_id = 2 THEN 'User -> Admin'
        ELSE 'Other'
    END as direction,
    CASE 
        WHEN LEN(CAST(message AS NVARCHAR(MAX))) > 50 
        THEN SUBSTRING(CAST(message AS NVARCHAR(MAX)), 1, 50) + '...'
        ELSE CAST(message AS NVARCHAR(MAX))
    END as message_text,
    CASE 
        WHEN has_attachments = 1 THEN 'Yes'
        ELSE 'No'
    END as has_file
FROM chat_messages 
WHERE (sender_id = 1 AND receiver_id = 2) OR (sender_id = 2 AND receiver_id = 1)
ORDER BY timestamp ASC;

PRINT ''
PRINT '--- VALIDATION ---'

-- Check if messages are in correct chronological order
DECLARE @order_correct BIT = 1
DECLARE @prev_timestamp DATETIME2 = '1900-01-01'
DECLARE @current_timestamp DATETIME2

DECLARE timestamp_cursor CURSOR FOR
SELECT timestamp FROM chat_messages ORDER BY message_id ASC

OPEN timestamp_cursor
FETCH NEXT FROM timestamp_cursor INTO @current_timestamp

WHILE @@FETCH_STATUS = 0
BEGIN
    IF @current_timestamp < @prev_timestamp
    BEGIN
        SET @order_correct = 0
        BREAK
    END
    SET @prev_timestamp = @current_timestamp
    FETCH NEXT FROM timestamp_cursor INTO @current_timestamp
END

CLOSE timestamp_cursor
DEALLOCATE timestamp_cursor

IF @order_correct = 1
    PRINT '✅ Messages are in correct chronological order'
ELSE
    PRINT '❌ Messages are NOT in chronological order'

PRINT ''
PRINT '--- SUMMARY ---'

DECLARE @total_messages INT
SELECT @total_messages = COUNT(*) FROM chat_messages

PRINT 'Total messages: ' + CAST(@total_messages AS VARCHAR(10))
PRINT 'Expected UI order: Oldest (top) -> Newest (bottom)'
PRINT 'Auto-scroll should show newest message at bottom'

PRINT ''
PRINT '=== TEST COMPLETED ==='
