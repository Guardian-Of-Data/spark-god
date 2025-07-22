// JavaScript code to add Hello tab to Spark UI
// Run this in the browser console when viewing Spark Web UI

function addHelloTab() {
  // Find the navigation tabs
  const navTabs = document.querySelector('.nav-tabs');
  if (!navTabs) {
    console.log('Navigation tabs not found');
    return;
  }
  
  // Create new tab
  const newTab = document.createElement('li');
  newTab.className = 'nav-item';
  newTab.innerHTML = `
    <a class="nav-link" href="#hello" data-toggle="tab" role="tab">
      Hello
    </a>
  `;
  
  // Add tab to navigation
  navTabs.appendChild(newTab);
  
  // Find the tab content container
  const tabContent = document.querySelector('.tab-content');
  if (!tabContent) {
    console.log('Tab content not found');
    return;
  }
  
  // Create new tab content
  const newTabContent = document.createElement('div');
  newTabContent.className = 'tab-pane fade';
  newTabContent.id = 'hello';
  newTabContent.innerHTML = `
    <div class="container-fluid">
      <div class="row">
        <div class="col-12">
          <div class="card">
            <div class="card-header">
              <h3>Hello World</h3>
            </div>
            <div class="card-body">
              <h1>Hello, World!</h1>
              <p>This is a custom Spark UI tab created by the plugin.</p>
              <p>Current time: ${new Date().toLocaleString()}</p>
              <p>This tab was added dynamically using JavaScript.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  `;
  
  // Add content to tab container
  tabContent.appendChild(newTabContent);
  
  // Add click handler to show the tab
  newTab.querySelector('a').addEventListener('click', function(e) {
    e.preventDefault();
    
    // Hide all tab contents
    document.querySelectorAll('.tab-pane').forEach(pane => {
      pane.classList.remove('active', 'show');
    });
    
    // Remove active class from all tabs
    document.querySelectorAll('.nav-link').forEach(link => {
      link.classList.remove('active');
    });
    
    // Show this tab
    newTabContent.classList.add('active', 'show');
    this.classList.add('active');
  });
  
  console.log('Hello tab added successfully!');
}

// Auto-run when page loads
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', addHelloTab);
} else {
  addHelloTab();
}

console.log('Hello tab script loaded. Run addHelloTab() to add the tab manually.'); 