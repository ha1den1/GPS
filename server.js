const express = require('express');
const fs = require('fs');
const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

app.post('/save-location', (req, res) => {
    const { latitude, longitude, timestamp } = req.body; // Extract timestamp from the request body

    // Create a string representation of the location data with timestamp
    const locationString = `${timestamp}: ${latitude},${longitude}\n`;

    // Path to the text file where you want to save the data
    const filePath = 'location_data.txt';

    // Append the location data to the text file
    fs.appendFile(filePath, locationString, (err) => {
        if (err) {
            console.error('Error saving location to file:', err);
            res.status(500).send('Error saving location');
        } else {
            console.log('Location saved to file:', locationString);
            res.send('Location received and saved');
        }
    });
});

app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
