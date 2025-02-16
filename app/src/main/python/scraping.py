from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from bs4 import BeautifulSoup
import time

# Set up Chrome options (optional)
chrome_options = Options()
chrome_options.add_argument("--headless")

# Path to your ChromeDriver executable
chrome_driver_path = r'C:/Users/varda/Downloads/chromedriver-win64/chromedriver-win64/chromedriver.exe'

# Set up the WebDriver for Chrome
service = Service(chrome_driver_path)
driver = webdriver.Chrome(service=service, options=chrome_options)

# Open the target URL
url = "https://unogs.com/genre/11714/TV%20Dramas"
driver.get(url)

# Wait for the page to load initially
time.sleep(3)

# Set the number of scrolls you want to simulate
scroll_count = 10

# Scroll down multiple times to load more content
for _ in range(scroll_count):
    driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
    time.sleep(3)  # Adjust sleep time if necessary

# Get the page source after scrolling
html_content = driver.page_source

# Initialize BeautifulSoup with the rendered HTML content
soup = BeautifulSoup(html_content, 'html.parser')

# Extract titles and image links
movie_data = []
titles = soup.find_all('span', {'data-bind': 'html:title'})  # Title selector
images = soup.find_all('img', {'class': 'img-rounded'})  # Image selector

# Append titles and images to the movie_data list as tuples
for i, title in enumerate(titles):
    title_text = title.text.strip()
    if i < len(images):  # Ensure the index is within bounds
        image_url = images[i].get('src', "No image found")  # Safely get the 'src' attribute
    else:
        image_url = "No image found"

    # Append the title and image as a tuple
    movie_data.append((title_text, image_url))

# Now movie_data contains the list of tuples with titles and image URLs
return (movie_data)

# Close the browser session
driver.quit()