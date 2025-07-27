# JavaScript API

This document explains how to execute FancyMenu actions from web pages loaded in the MCEF browser.

When a web page is loaded in a FancyMenu MCEF browser, a JavaScript API is automatically injected that allows the page to execute FancyMenu actions. This enables interactive web content that can control various aspects of the game.

The API is available through two global objects:
- `window.fancymenu` - The primary namespace
- `window.FancyMenu` - An alias for convenience

# Methods

## Without Callback: `fancymenu.execute(actionType, actionValue)`
Executes a FancyMenu action without callbacks.

**Parameters:**
- `actionType` (string) - The type of action to execute
- `actionValue` (string, optional) - The value for the action (omit for actions without values)

**Examples:**
```javascript
// Action without value
fancymenu.execute('quitgame');

// Action with value
fancymenu.execute('opengui', 'title_screen');
fancymenu.execute('set_variable', 'myvar:myvalue');
```

## With Callback: `fancymenu.executeWithCallback(actionType, actionValue, onSuccess, onFailure)`
Executes a FancyMenu action with callback functions.

**Parameters:**
- `actionType` (string) - The type of action to execute
- `actionValue` (string, optional) - The value for the action
- `onSuccess` (function, optional) - Called when the action executes successfully
- `onFailure` (function, optional) - Called when the action fails

**Note:** For actions without values, you can pass the callbacks as the second and third arguments.

**Examples:**
```javascript
// Action without value
fancymenu.executeWithCallback('quitgame',
    function(result) {
        console.log('Game quit initiated');
    },
    function(error) {
        console.error('Failed to quit:', error);
    }
);

// Action with value
fancymenu.executeWithCallback('opengui', 'title_screen',
    function(result) {
        console.log('Title screen opened');
    },
    function(error) {
        console.error('Failed to open GUI:', error);
    }
);
```

# Common Action Types

## Actions without values:
- `quitgame` - Quits the game
- `back_to_last_screen` - Returns to the previous screen

## Actions with values:
- `opengui` - Opens a specific GUI screen
  - Example: `fancymenu.execute('opengui', 'title_screen')`
- `openlink` - Opens a URL in the default browser
  - Example: `fancymenu.execute('openlink', 'https://minecraft.net')`
- `sendmessage` - Sends a chat message
  - Example: `fancymenu.execute('sendmessage', 'Hello World!')`
- `set_variable` - Sets a FancyMenu variable
  - Example: `fancymenu.execute('set_variable', 'myvar:myvalue')`
- `joinserver` - Connects to a Minecraft server
  - Example: `fancymenu.execute('joinserver', 'play.hypixel.net')`
- `disconnect_server_or_world` - Disconnects from current server or world and opens a specific screen after
  - Example: `fancymenu.execute('disconnect_server_or_world', 'title_screen')`

# Example HTML

```html
<!DOCTYPE html>
<html>
<head>
    <title>FancyMenu Integration</title>
</head>
<body>
    <h1>Game Controls</h1>
    
    <button onclick="quitGame()">Quit Game</button>
    <button onclick="openTitleScreen()">Title Screen</button>
    <button onclick="disconnectFromServer()">Disconnect</button>
    <button onclick="setVariable()">Set Variable</button>
    
    <script>
        function quitGame() {
            if (typeof fancymenu !== 'undefined') {
                fancymenu.execute('quitgame');
            }
        }
        
        function openTitleScreen() {
            if (typeof fancymenu !== 'undefined') {
                fancymenu.executeWithCallback('opengui', 'title_screen',
                    function() { console.log('Title screen opened!'); },
                    function(err) { console.error('Error:', err); }
                );
            }
        }
        
        function disconnectFromServer() {
            if (typeof fancymenu !== 'undefined') {
                fancymenu.execute('disconnect_server_or_world', 'title_screen');
            }
        }
        
        function setVariable() {
            if (typeof fancymenu !== 'undefined') {
                var varName = prompt('Variable name:');
                var varValue = prompt('Variable value:');
                if (varName && varValue) {
                    fancymenu.execute('set_variable', varName + ':' + varValue);
                }
            }
        }
    </script>
</body>
</html>
```

# Best Practices

1. **Check API Availability**: Always check if `fancymenu` is defined before using it
2. **Handle Errors**: Use the callback version (`executeWithCallback`) for important actions
3. **Validate Input**: Sanitize user input before passing to actions
4. **Action Values**: Remember that some actions require values while others don't
5. **Performance**: Avoid executing too many actions rapidly

# Security Notes

- Actions are executed with the same permissions as if triggered from the game UI
- Some actions may be restricted based on game state
- Always validate and sanitize user input to prevent injection attacks
- The `set_variable` action value format is `variable_name:variable_value`

# Troubleshooting

If the API is not available:
1. Ensure the page is loaded in a FancyMenu MCEF browser
2. Check the browser console for errors
3. Verify that JavaScript is enabled

For action-specific issues:
1. Check that you're using the correct action type
2. Verify if the action requires a value or not
3. Ensure any required values are properly formatted
4. Check the game logs for any error messages

# Events

The API dispatches a `fancymenu-ready` event when it's fully loaded:

```javascript
window.addEventListener('fancymenu-ready', function() {
    console.log('FancyMenu API is ready!');
    // Your code here
});
```
