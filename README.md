# Content Report React Module

A comprehensive content reporting tool for Jahia DX, built with React and the Moonstone UI library. This module provides powerful insights into your Jahia site's content through various customizable reports.

## Overview

The Content Report React module offers a modern, user-friendly interface for generating and viewing detailed reports about your Jahia site content. It features a site overview dashboard and multiple specialized reports covering content, languages, visibility, metadata, and system information.

## Features

### 📊 Site Overview Dashboard
- **Content Metrics**: Pages, Templates, Users, Content nodes, Editorial content
- **Workflow Tracking**: Pending workflow tasks count
- **Asset Management**: Files and Images counts
- **Language Support**: Visual display of available site languages with flag emojis
- **Content Activity (Last 30 Days)**:
  - New content created
  - Modified content items
  - Published content items
  - Published vs Unpublished nodes comparison
  - Average time from creation to publication
  - Top 5 contributors ranked by content count (with medal badges 🥇🥈🥉)

### 📝 Content Reports
- **By Author and Date**: Filter content by author, creation/modification dates
- **By Content Type**: Analyze content distribution by type
- **By Status**: View content by publication status
- **Marked for Deletion**: Track content scheduled for removal
- **Unreferenced Assets**: Audit files under `/sites/<site>/files` that are not referenced anywhere, with direct links into media folders and inline previews for images
- **Live Contents**: Report on published content
- **Expired/Future Content**: Monitor time-sensitive content

### 🌍 Language Reports
- **Pages Without Title**: Identify pages missing titles in specific languages
- **Untranslated Pages**: Find pages not translated to specific languages
- **Pages Without Keywords/Description**: SEO-focused reports

### 🔒 Visibility & Security Reports
- **ACL Inheritance Break**: Detect broken ACL inheritance and DENY permissions
- **Locked Content**: View currently locked content

### ⚙️ System Reports
- **Custom Cache Content**: Track custom-cached content
- **Workflow Tasks**: Monitor pending workflow tasks

## Requirements

- **Jahia DX**: 8.2.0.0 or higher
- **Java**: 11 or higher
- **Node.js**: 20.14.0 (via frontend-maven-plugin)
- **Yarn**: 1.22.19

## Installation

### From Source

1. Clone the repository:
```bash
git clone https://github.com/smonier/contentReportReact.git
cd contentReportReact
```

2. Build the module:
```bash
mvn clean install
```

3. Deploy the generated JAR to your Jahia server:
```bash
cp target/contentReportReact-1.0.0-SNAPSHOT.jar $JAHIA_HOME/modules/
```

### From Binary

1. Download the latest release JAR
2. Copy to your Jahia modules directory
3. Restart Jahia or deploy via Module Management

## Usage

### Accessing the Module

1. Log in to Jahia as an administrator
2. Navigate to **Additional** From your Site jContent
3. Select **Content Report** from the settings menu

### Running Reports

1. **Select a Report**: Choose from the categorized menu on the left
2. **Configure Parameters**: 
   - Set the content path (use Browse button for picker)
   - Configure date ranges, filters, and other options
   - Enable/disable optional filters via checkboxes
3. **Execute**: Click "Run Report" button
4. **View Results**: Results display in a sortable, paginated table

### Report Categories

- **Overview**: Site-wide statistics and metrics
- **Content**: Content analysis and filtering reports
- **Languages**: Translation and localization reports
- **Visibility**: Publication status and access control reports
- **Metadata**: SEO and metadata completeness reports
- **System**: Technical and administrative reports

## Report Columns

Reports support various column types:
- **Text**: Standard text display
- **Date**: Formatted date/time values
- **Boolean**: Checkmark/cross indicators
- **Link**: Clickable content links
- **HTML**: Rich formatted content
- **i18n**: Internationalized property values

## Configuration

### Custom Reports

Reports are configured in `src/javascript/AdminPanel/AdminPanel.constants.js`:

```javascript
{
    id: 'myReport',
    labelKey: 'menu.myReport',
    descriptionKey: 'descriptions.myReport',
    type: 'legacy',
    category: 'content',
    fields: [
        {name: 'pathTxt', type: 'path', labelKey: 'fields.path', required: true},
        // ... more fields
    ],
    columns: [
        {key: 'title', labelKey: 'columns.title', sortable: true},
        // ... more columns
    ]
}
```

### Translations

The module supports 6 languages with complete translations:
- `src/main/resources/javascript/locales/en.json` (English)
- `src/main/resources/javascript/locales/fr.json` (French)
- `src/main/resources/javascript/locales/de.json` (German)
- `src/main/resources/javascript/locales/es.json` (Spanish)
- `src/main/resources/javascript/locales/it.json` (Italian)
- `src/main/resources/javascript/locales/pt.json` (Portuguese)

**Content Activity Translation Keys** (added for all languages):
```json
{
  "result": {
    "contentActivity": "Content Activity (Last 30 Days)",
    "newContentLast30Days": "New Content Created",
    "modifiedContentLast30Days": "Modified Content Items",
    "publishedContentLast30Days": "Published Content Items",
    "unpublishedNodes": "Unpublished Nodes",
    "publishedNodes": "Published Nodes",
    "averageTimeToPublish": "Average Time to Publish",
    "days": "days",
    "topContributors": "Top Contributors",
    "items": "items",
    "noContributorsData": "No contributors data available for the last 30 days"
  }
}
```

### Backend Reports

Create custom report beans by extending `BaseReport` or `QueryReport`:

```java
@Component
public class MyCustomReport extends QueryReport {
    @Override
    public String getId() {
        return "myReport";
    }
    
    @Override
    protected String buildQuery(Map<String, String> parameters) {
        // Build JCR SQL-2 query
        return "SELECT * FROM [jnt:page] WHERE ISDESCENDANTNODE('" + path + "')";
    }
}
```

## Development

### Project Structure

```
contentReportReact/
├── src/
│   ├── javascript/          # React frontend
│   │   ├── AdminPanel/      # Main UI components
│   │   │   ├── reports/     # Report-specific components
│   │   │   └── AdminPanel.constants.js
│   │   ├── graphql/         # GraphQL queries
│   │   └── styles/          # SCSS styles
│   └── main/
│       ├── java/            # Java backend
│       │   └── org/jahia/modules/contentreports/
│       │       ├── bean/    # Report implementations
│       │       ├── graphql/ # GraphQL resolvers
│       │       └── service/ # Business logic
│       └── resources/
│           ├── definitions/ # GraphQL schema
│           └── javascript/  # Built assets
│               ├── apps/    # Webpack output
│               └── locales/ # i18n files
├── package.json
├── pom.xml
└── webpack.config.js
```

### Building for Development

```bash
# Install dependencies
yarn install

# Development mode with watch
yarn dev

# Production build
yarn build:production

# Lint code
yarn lint
yarn lint:fix

# Clean build artifacts
yarn clean
```

### Adding a New Report

1. **Define Report Configuration** (`AdminPanel.constants.js`):
   ```javascript
   {
       id: 'newReport',
       labelKey: 'menu.newReport',
       type: 'legacy',
       category: 'content',
       fields: [...],
       columns: [...]
   }
   ```

2. **Add Translations** (`en.json`, `fr.json`):
   ```json
   {
       "menu": {
           "newReport": "New Report"
       },
       "descriptions": {
           "newReport": "Description of the new report"
       }
   }
   ```

3. **Create Backend Report** (Java):
   ```java
   @Component
   public class ReportNewReport extends QueryReport {
       @Override
       public String getId() {
           return "newReport";
       }
       
       @Override
       protected String buildQuery(Map<String, String> parameters) {
           // Implement query logic
       }
   }
   ```

4. **Build and Test**:
   ```bash
   mvn clean install -DskipTests
   ```

## GraphQL API

The module exposes GraphQL queries for accessing reports:

```graphql
query {
  admin {
    contentReports {
      overview(siteKey: "/sites/mysite", language: "en") {
        siteName
        nbPages
        nbContents
        nbFiles
        nbImages
        languages
        # Content Activity (Last 30 Days)
        newContentLast30Days
        modifiedContentLast30Days
        publishedContentLast30Days
        unpublishedNodes
        publishedNodes
        averageTimeToPublish
        topContributors {
          username
          contentCount
        }
      }
      rawReport(
        siteKey: "mysite"
        language: "en"
        reportId: "20"
        offset: 0
        limit: 100
        parameterName1: "pathTxt"
        parameterValue1: "/sites/mysite"
      )
    }
  }
}
```

## Technologies

### Frontend
- **React** 18.2.0: UI framework
- **Moonstone** 2.16.2: Jahia's design system
- **i18next**: Internationalization
- **Axios**: HTTP client
- **Webpack** 5: Module bundler

### Backend
- **Java** 11: Backend language
- **GraphQL**: API layer (graphql-java-annotations)
- **JCR**: Content repository queries
- **Spring**: Dependency injection
- **OSGi**: Module framework

## Performance Considerations

- Reports use `rep:count(item, skipChecks=1)` for efficient counting
- Client-side pagination for large datasets (default limit: 10,000)
- Lazy loading of report results
- Optimized JCR queries with proper indexing

## Troubleshooting

### Build Issues

**Problem**: Frontend build fails
```bash
yarn clean:all
yarn install
mvn clean install
```

**Problem**: Java compilation errors
- Ensure Java 11+ is installed
- Check Jahia parent POM version compatibility

### Runtime Issues

**Problem**: Reports return no data
- Verify user has read permissions on content
- Check JCR query syntax in logs
- Ensure site key and language are correct

**Problem**: Path picker not working
- Verify Content Editor API is available
- Check browser console for JavaScript errors

### Debugging

Enable debug logging in Jahia:
```
log4j.logger.org.jahia.modules.contentreports=DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -am 'Add new feature'`
4. Push to branch: `git push origin feature/my-feature`
5. Submit a pull request

### Code Standards

- Follow ESLint rules for JavaScript
- Use Java code formatting standards
- Add translations for all new strings
- Document complex logic with comments
- Write meaningful commit messages

## License

MIT License

Copyright (c) 2002 - 2022 Jahia Solutions Group. All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Support

For issues, questions, or contributions:
- GitHub Issues: [https://github.com/smonier/contentReportReact/issues](https://github.com/smonier/contentReportReact/issues)
- Jahia Community: [https://community.jahia.com](https://community.jahia.com)

## Changelog

### Version 1.0.0-SNAPSHOT
- Initial release
- 12+ pre-configured reports
- Site overview dashboard with comprehensive metrics
- Multi-language support (EN, FR, DE, ES, IT, PT)
- Modern React UI with Moonstone design system
- GraphQL API for report access
- Customizable report framework
- Asset tracking (files and images)
- Workflow task monitoring
- ACL inheritance break detection
- **Content Activity Analytics (Last 30 Days)**:
  - New content created tracking
  - Modified content items monitoring
  - Published content items tracking
  - Published vs unpublished nodes comparison
  - Average time from creation to publication metric
  - Top 5 contributors with ranking (medal badges)

## Roadmap

- [ ] Export reports to CSV/Excel
- [ ] Scheduled report generation
- [ ] Email notifications for report results
- [ ] Custom report builder UI
- [ ] Advanced filtering and search
- [ ] Report templates and presets
- [ ] Historical data tracking
- [ ] Additional language support

---

**Made with ❤️ for the Jahia Community**
