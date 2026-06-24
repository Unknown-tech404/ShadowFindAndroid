#!/usr/bin/env python3
import sys
import re
import time
import json
import hashlib
import requests
from bs4 import BeautifulSoup
from urllib.parse import urlparse, urljoin
from concurrent.futures import ThreadPoolExecutor

class ShadowFindAndroid:
    def __init__(self):
        self.target_url = None
        self.domain = None
        self.visited_urls = set()
        self.all_links = set()
        self.internal_links = set()
        self.external_links = set()
        self.images = set()
        self.scripts = set()
        self.stylesheets = set()
        self.forms = set()
        self.emails = set()
        self.phone_numbers = set()
        self.lock = threading.Lock()
        
    def authenticate(self, username, password):
        """Authenticate user"""
        if username == "Shadow" and password == "Find":
            return True
        return False
        
    def get_page_content(self, url):
        try:
            response = requests.get(
                url,
                timeout=10,
                headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'},
                verify=False
            )
            if response.status_code == 200:
                return response.text
        except:
            pass
        return None
    
    def normalize_url(self, url):
        if not url:
            return None
        try:
            parsed = urlparse(url)
            normalized = urlunparse((
                parsed.scheme.lower(),
                parsed.netloc.lower(),
                parsed.path.rstrip('/') or '/',
                parsed.params,
                parsed.query,
                ''
            ))
            return normalized
        except:
            return url
    
    def is_internal_link(self, link):
        if not link:
            return False
        try:
            parsed = urlparse(link)
            if not parsed.netloc:
                return True
            return parsed.netloc == self.domain
        except:
            return False

    def extract_links_from_page(self, url, depth):
        with self.lock:
            if url in self.visited_urls:
                return []
            self.visited_urls.add(url)
        
        content = self.get_page_content(url)
        if not content:
            return []
        
        discovered_internals = []
        link_patterns = [
            r'href\s*=\s*["\']([^"\']+)["\']',
            r'src\s*=\s*["\']([^"\']+)["\']',
            r'action\s*=\s*["\']([^"\']+)["\']',
        ]
        
        for pattern in link_patterns:
            for match in re.findall(pattern, content, re.IGNORECASE):
                href_str = match.strip()
                if not href_str or href_str.startswith(('javascript:', 'mailto:', 'tel:', '#')):
                    continue
                    
                full_url = urljoin(url, href_str)
                normalized = self.normalize_url(full_url)
                if normalized:
                    with self.lock:
                        if normalized not in self.all_links:
                            self.all_links.add(normalized)
                            if self.is_internal_link(normalized):
                                self.internal_links.add(normalized)
                                discovered_internals.append(normalized)
                            else:
                                self.external_links.add(normalized)
        
        for email in re.findall(r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}', content):
            self.emails.add(email)
            
        for phone in re.findall(r'\+?[0-9]{1,4}[-\s\.]?\(?[0-9]{1,4}\)?[-\s\.]?[0-9]{1,4}[-\s\.]?[0-9]{1,4}', content):
            if len(phone.strip()) >= 7:
                self.phone_numbers.add(phone.strip())
        
        for match in re.findall(r'src\s*=\s*["\']([^"\']+\.(?:png|jpg|jpeg|gif|svg|webp))["\']', content, re.IGNORECASE):
            self.images.add(urljoin(url, match))
        for match in re.findall(r'src\s*=\s*["\']([^"\']+\.js)["\']', content, re.IGNORECASE):
            self.scripts.add(urljoin(url, match))
            
        return discovered_internals
    
    def scan(self, url, threads=10, max_depth=2):
        if not url.startswith(('http://', 'https://')):
            url = 'https://' + url
        
        self.target_url = url
        self.domain = urlparse(url).netloc
        start_time = time.time()
        
        current_layer_queue = [url]
        
        for current_depth in range(max_depth + 1):
            if not current_layer_queue:
                break
                
            next_layer_queue = []
            with ThreadPoolExecutor(max_workers=threads) as executor:
                futures = {executor.submit(self.extract_links_from_page, target, current_depth): target for target in current_layer_queue}
                for future in futures:
                    try:
                        results = future.result()
                        if results:
                            next_layer_queue.extend(results)
                    except:
                        pass
            
            current_layer_queue = list(set(next_layer_queue) - self.visited_urls)
        
        end_time = time.time()
        scan_time = end_time - start_time
        
        return {
            'total_links': len(self.all_links),
            'internal_links': len(self.internal_links),
            'external_links': len(self.external_links),
            'images': len(self.images),
            'scripts': len(self.scripts),
            'stylesheets': len(self.stylesheets),
            'forms': len(self.forms),
            'emails': len(self.emails),
            'phone_numbers': len(self.phone_numbers),
            'scan_time': scan_time,
            'links': sorted(list(self.all_links))[:100]  # Limit to 100 for display
        }

def scan_android(url, threads, depth, username, password):
    """Main entry point for Android"""
    finder = ShadowFindAndroid()
    
    # Authenticate
    if not finder.authenticate(username, password):
        return json.dumps({
            'success': False,
            'error': 'Authentication failed'
        })
    
    try:
        results = finder.scan(url, threads, depth)
        return json.dumps({
            'success': True,
            'data': results
        })
    except Exception as e:
        return json.dumps({
            'success': False,
            'error': str(e)
        })
