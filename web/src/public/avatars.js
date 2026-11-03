/**
 * Fitness Ecosystem Pro — Default Avatar Catalog
 * 12 Swiss Clean Vector SVGs (6 Characters + 6 Equipment)
 * Strict Zero-Mocks & WCAG AAA Contrast
 */

(function (global) {
  const AVATAR_CATALOG = [
    // --- Персонажи (6) ---
    {
      id: 'male-athlete',
      title: 'Мужчина-атлет',
      category: 'character',
      categoryName: 'Персонажи',
      themeColor: '#0f172a',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#0f172a"/><circle cx="32" cy="32" r="31.5" stroke="#334155" stroke-opacity="0.6"/><path d="M12 58C12 45 20.5 41 24 39.5L27 46H37L40 39.5C43.5 41 52 45 52 58V64H12V58Z" fill="#10b981"/><path d="M27 40C27 43.5 29.2 47 32 47C34.8 47 37 43.5 37 40V37H27V40Z" fill="#0f172a"/><path d="M28 32H36V40C36 41.5 34.2 43 32 43C29.8 43 28 41.5 28 40V32Z" fill="#f1f5f9"/><circle cx="32" cy="24" r="9.5" fill="#f1f5f9"/><path d="M22.5 23C22.5 17.5 26.5 14 32 14C37.5 14 41.5 17.5 41.5 23C41.5 24 41 25 40 25C39 20.5 36.5 18 32 18C27.5 18 24.5 20.5 24 25C23 25 22.5 24 22.5 23Z" fill="#1e293b"/></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%230f172a%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%23334155%22%20stroke-opacity%3D%220.6%22%2F%3E%3Cpath%20d%3D%22M12%2058C12%2045%2020.5%2041%2024%2039.5L27%2046H37L40%2039.5C43.5%2041%2052%2045%2052%2058V64H12V58Z%22%20fill%3D%22%2310b981%22%2F%3E%3Cpath%20d%3D%22M27%2040C27%2043.5%2029.2%2047%2032%2047C34.8%2047%2037%2043.5%2037%2040V37H27V40Z%22%20fill%3D%22%230f172a%22%2F%3E%3Cpath%20d%3D%22M28%2032H36V40C36%2041.5%2034.2%2043%2032%2043C29.8%2043%2028%2041.5%2028%2040V32Z%22%20fill%3D%22%23f1f5f9%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2224%22%20r%3D%229.5%22%20fill%3D%22%23f1f5f9%22%2F%3E%3Cpath%20d%3D%22M22.5%2023C22.5%2017.5%2026.5%2014%2032%2014C37.5%2014%2041.5%2017.5%2041.5%2023C41.5%2024%2041%2025%2040%2025C39%2020.5%2036.5%2018%2032%2018C27.5%2018%2024.5%2020.5%2024%2025C23%2025%2022.5%2024%2022.5%2023Z%22%20fill%3D%22%231e293b%22%2F%3E%3C%2Fsvg%3E'
    },
    {
      id: 'female-athlete',
      title: 'Девушка-атлетка',
      category: 'character',
      categoryName: 'Персонажи',
      themeColor: '#064e3b',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#064e3b"/><circle cx="32" cy="32" r="31.5" stroke="#047857" stroke-opacity="0.4"/><path d="M14 58C14 46 22 42 25 41L28 47H36L39 41C42 42 50 46 50 58V64H14V58Z" fill="#34d399"/><path d="M26 41L28 47M38 41L36 47" stroke="#064e3b" stroke-width="2" stroke-linecap="round"/><path d="M29 32H35V40C35 41.5 33.7 42.5 32 42.5C30.3 42.5 29 41.5 29 40V32Z" fill="#f8fafc"/><circle cx="32" cy="24" r="8.5" fill="#f8fafc"/><path d="M23.5 23C23.5 18 27.5 15 32 15C36.5 15 40.5 18 40.5 23C40.5 23.5 39 21 32 21C25 21 23.5 23.5 23.5 23Z" fill="#0f172a"/><path d="M37 18C41 16 46 17 48 21C49 23 48.5 27 46 30C45 31 43.5 29 44 26C44.5 23 43 20 37 18Z" fill="#0f172a"/><circle cx="38" cy="18" r="2" fill="#34d399"/></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%23064e3b%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%23047857%22%20stroke-opacity%3D%220.4%22%2F%3E%3Cpath%20d%3D%22M14%2058C14%2046%2022%2042%2025%2041L28%2047H36L39%2041C42%2042%2050%2046%2050%2058V64H14V58Z%22%20fill%3D%22%2334d399%22%2F%3E%3Cpath%20d%3D%22M26%2041L28%2047M38%2041L36%2047%22%20stroke%3D%22%23064e3b%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%2F%3E%3Cpath%20d%3D%22M29%2032H35V40C35%2041.5%2033.7%2042.5%2032%2042.5C30.3%2042.5%2029%2041.5%2029%2040V32Z%22%20fill%3D%22%23f8fafc%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2224%22%20r%3D%228.5%22%20fill%3D%22%23f8fafc%22%2F%3E%3Cpath%20d%3D%22M23.5%2023C23.5%2018%2027.5%2015%2032%2015C36.5%2015%2040.5%2018%2040.5%2023C40.5%2023.5%2039%2021%2032%2021C25%2021%2023.5%2023.5%2023.5%2023Z%22%20fill%3D%22%230f172a%22%2F%3E%3Cpath%20d%3D%22M37%2018C41%2016%2046%2017%2048%2021C49%2023%2048.5%2027%2046%2030C45%2031%2043.5%2029%2044%2026C44.5%2023%2043%2020%2037%2018Z%22%20fill%3D%22%230f172a%22%2F%3E%3Ccircle%20cx%3D%2238%22%20cy%3D%2218%22%20r%3D%222%22%20fill%3D%22%2334d399%22%2F%3E%3C%2Fsvg%3E'
    },
    {
      id: 'male-coach',
      title: 'Тренер Pro',
      category: 'character',
      categoryName: 'Персонажи',
      themeColor: '#0c4a6e',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#0c4a6e"/><circle cx="32" cy="32" r="31.5" stroke="#0284c7" stroke-opacity="0.4"/><path d="M12 58C12 45 21 41 24 39.5L27 45H37L40 39.5C43 41 52 45 52 58V64H12V58Z" fill="#0284c7"/><path d="M26 40L32 45L38 40" stroke="#f8fafc" stroke-width="2" stroke-linejoin="round"/><path d="M29 42L32 50L35 42" stroke="#f59e0b" stroke-width="1.5" fill="none"/><path d="M30.5 50H33.5V53H32C31 53 30.5 52 30.5 50Z" fill="#f59e0b"/><path d="M28 32H36V40C36 41.5 34.2 43 32 43C29.8 43 28 41.5 28 40V32Z" fill="#f1f5f9"/><circle cx="32" cy="24" r="9.5" fill="#f1f5f9"/><path d="M21 21C21 16 25.5 13 32 13C38.5 13 43 16 43 21H21Z" fill="#0f172a"/><path d="M30 19H45C46 19 46.5 20.5 45.5 21.5L42 23H30V19Z" fill="#38bdf8"/></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%230c4a6e%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%230284c7%22%20stroke-opacity%3D%220.4%22%2F%3E%3Cpath%20d%3D%22M12%2058C12%2045%2021%2041%2024%2039.5L27%2045H37L40%2039.5C43%2041%2052%2045%2052%2058V64H12V58Z%22%20fill%3D%22%230284c7%22%2F%3E%3Cpath%20d%3D%22M26%2040L32%2045L38%2040%22%20stroke%3D%22%23f8fafc%22%20stroke-width%3D%222%22%20stroke-linejoin%3D%22round%22%2F%3E%3Cpath%20d%3D%22M29%2042L32%2050L35%2042%22%20stroke%3D%22%23f59e0b%22%20stroke-width%3D%221.5%22%20fill%3D%22none%22%2F%3E%3Cpath%20d%3D%22M30.5%2050H33.5V53H32C31%2053%2030.5%2052%2030.5%2050Z%22%20fill%3D%22%23f59e0b%22%2F%3E%3Cpath%20d%3D%22M28%2032H36V40C36%2041.5%2034.2%2043%2032%2043C29.8%2043%2028%2041.5%2028%2040V32Z%22%20fill%3D%22%23f1f5f9%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2224%22%20r%3D%229.5%22%20fill%3D%22%23f1f5f9%22%2F%3E%3Cpath%20d%3D%22M21%2021C21%2016%2025.5%2013%2032%2013C38.5%2013%2043%2016%2043%2021H21Z%22%20fill%3D%22%230f172a%22%2F%3E%3Cpath%20d%3D%22M30%2019H45C46%2019%2046.5%2020.5%2045.5%2021.5L42%2023H30V19Z%22%20fill%3D%22%2338bdf8%22%2F%3E%3C%2Fsvg%3E'
    },
    {
      id: 'female-coach',
      title: 'Фитнес-тренер',
      category: 'character',
      categoryName: 'Персонажи',
      themeColor: '#134e4a',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#134e4a"/><circle cx="32" cy="32" r="31.5" stroke="#2dd4bf" stroke-opacity="0.3"/><path d="M13 58C13 46 21 42 25 40L28 46H36L39 40C43 42 51 46 51 58V64H13V58Z" fill="#14b8a6"/><circle cx="32" cy="50" r="3" fill="#f0fdfa"/><path d="M29 32H35V39C35 41 33.7 42 32 42C30.3 42 29 41 29 39V32Z" fill="#f8fafc"/><circle cx="32" cy="24" r="8.5" fill="#f8fafc"/><circle cx="32" cy="12" r="4.5" fill="#0f172a"/><path d="M23 22C23 16.5 27 14 32 14C37 14 41 16.5 41 22C41 23 39 21.5 32 21.5C25 21.5 23 23 23 22Z" fill="#0f172a"/><path d="M23.5 19.5C25.5 17.5 28.5 16.5 32 16.5C35.5 16.5 38.5 17.5 40.5 19.5" stroke="#5eead4" stroke-width="2.5" stroke-linecap="round"/></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%23134e4a%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%232dd4bf%22%20stroke-opacity%3D%220.3%22%2F%3E%3Cpath%20d%3D%22M13%2058C13%2046%2021%2042%2025%2040L28%2046H36L39%2040C43%2042%2051%2046%2051%2058V64H13V58Z%22%20fill%3D%22%2314b8a6%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2250%22%20r%3D%223%22%20fill%3D%22%23f0fdfa%22%2F%3E%3Cpath%20d%3D%22M29%2032H35V39C35%2041%2033.7%2042%2032%2042C30.3%2042%2029%2041%2029%2039V32Z%22%20fill%3D%22%23f8fafc%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2224%22%20r%3D%228.5%22%20fill%3D%22%23f8fafc%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2212%22%20r%3D%224.5%22%20fill%3D%22%230f172a%22%2F%3E%3Cpath%20d%3D%22M23%2022C23%2016.5%2027%2014%2032%2014C37%2014%2041%2016.5%2041%2022C41%2023%2039%2021.5%2032%2021.5C25%2021.5%2023%2023%2023%2022Z%22%20fill%3D%22%230f172a%22%2F%3E%3Cpath%20d%3D%22M23.5%2019.5C25.5%2017.5%2028.5%2016.5%2032%2016.5C35.5%2016.5%2038.5%2017.5%2040.5%2019.5%22%20stroke%3D%22%235eead4%22%20stroke-width%3D%222.5%22%20stroke-linecap%3D%22round%22%2F%3E%3C%2Fsvg%3E'
    },
    {
      id: 'powerlifter',
      title: 'Пауэрлифтер',
      category: 'character',
      categoryName: 'Персонажи',
      themeColor: '#27272a',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#27272a"/><circle cx="32" cy="32" r="31.5" stroke="#52525b" stroke-opacity="0.4"/><path d="M10 58C10 43 19 38 23 37L26 44H38L41 37C45 38 54 43 54 58V64H10V58Z" fill="#3f3f46"/><path d="M18 52H46V59H18V52Z" fill="#18181b"/><rect x="29" y="53" width="6" height="5" rx="1" stroke="#f59e0b" stroke-width="1.5" fill="none"/><path d="M26 31H38V38C38 41 35.5 43 32 43C28.5 43 26 41 26 38V31Z" fill="#f4f4f5"/><circle cx="32" cy="23" r="9.5" fill="#f4f4f5"/><path d="M22 21C22 15 26.5 13 32 13C37.5 13 42 15 42 21H22Z" fill="#18181b"/><path d="M24 23C24 28 27.5 32 32 32C36.5 32 40 28 40 23C40 25 38 29.5 32 29.5C26 29.5 24 25 24 23Z" fill="#18181b"/></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%2327272a%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%2352525b%22%20stroke-opacity%3D%220.4%22%2F%3E%3Cpath%20d%3D%22M10%2058C10%2043%2019%2038%2023%2037L26%2044H38L41%2037C45%2038%2054%2043%2054%2058V64H10V58Z%22%20fill%3D%22%233f3f46%22%2F%3E%3Cpath%20d%3D%22M18%2052H46V59H18V52Z%22%20fill%3D%22%2318181b%22%2F%3E%3Crect%20x%3D%2229%22%20y%3D%2253%22%20width%3D%226%22%20height%3D%225%22%20rx%3D%221%22%20stroke%3D%22%23f59e0b%22%20stroke-width%3D%221.5%22%20fill%3D%22none%22%2F%3E%3Cpath%20d%3D%22M26%2031H38V38C38%2041%2035.5%2043%2032%2043C28.5%2043%2026%2041%2026%2038V31Z%22%20fill%3D%22%23f4f4f5%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2223%22%20r%3D%229.5%22%20fill%3D%22%23f4f4f5%22%2F%3E%3Cpath%20d%3D%22M22%2021C22%2015%2026.5%2013%2032%2013C37.5%2013%2042%2015%2042%2021H22Z%22%20fill%3D%22%2318181b%22%2F%3E%3Cpath%20d%3D%22M24%2023C24%2028%2027.5%2032%2032%2032C36.5%2032%2040%2028%2040%2023C40%2025%2038%2029.5%2032%2029.5C26%2029.5%2024%2025%2024%2023Z%22%20fill%3D%22%2318181b%22%2F%3E%3C%2Fsvg%3E'
    },
    {
      id: 'runner',
      title: 'Бегун',
      category: 'character',
      categoryName: 'Персонажи',
      themeColor: '#082f49',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#082f49"/><circle cx="32" cy="32" r="31.5" stroke="#0284c7" stroke-opacity="0.3"/><path d="M14 58C14 47 21 42 25 40L28 46H36L39 40C43 42 50 47 50 58V64H14V58Z" fill="#0284c7"/><path d="M22 58L38 42H44L28 58H22Z" fill="#38bdf8"/><path d="M29 32H35V39C35 41 33.7 42 32 42C30.3 42 29 41 29 39V32Z" fill="#f8fafc"/><circle cx="32" cy="24" r="8.5" fill="#f8fafc"/><path d="M23 20H41V23H23V20Z" fill="#38bdf8"/><path d="M23.5 20C23.5 15.5 27 13 32 13C37 13 40.5 15.5 40.5 20H23.5Z" fill="#0f172a"/></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%23082f49%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%230284c7%22%20stroke-opacity%3D%220.3%22%2F%3E%3Cpath%20d%3D%22M14%2058C14%2047%2021%2042%2025%2040L28%2046H36L39%2040C43%2042%2050%2047%2050%2058V64H14V58Z%22%20fill%3D%22%230284c7%22%2F%3E%3Cpath%20d%3D%22M22%2058L38%2042H44L28%2058H22Z%22%20fill%3D%22%2338bdf8%22%2F%3E%3Cpath%20d%3D%22M29%2032H35V39C35%2041%2033.7%2042%2032%2042C30.3%2042%2029%2041%2029%2039V32Z%22%20fill%3D%22%23f8fafc%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2224%22%20r%3D%228.5%22%20fill%3D%22%23f8fafc%22%2F%3E%3Cpath%20d%3D%22M23%2020H41V23H23V20Z%22%20fill%3D%22%2338bdf8%22%2F%3E%3Cpath%20d%3D%22M23.5%2020C23.5%2015.5%2027%2013%2032%2013C37%2013%2040.5%2015.5%2040.5%2020H23.5Z%22%20fill%3D%22%230f172a%22%2F%3E%3C%2Fsvg%3E'
    },

    // --- Экипировка (6) ---
    {
      id: 'barbell',
      title: 'Штанга',
      category: 'equipment',
      categoryName: 'Экипировка',
      themeColor: '#09090b',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#09090b"/><circle cx="32" cy="32" r="31.5" stroke="#27272a"/><line x1="8" y1="32" x2="56" y2="32" stroke="#94a3b8" stroke-width="3" stroke-linecap="round"/><line x1="24" y1="32" x2="40" y2="32" stroke="#cbd5e1" stroke-width="3.5" stroke-linecap="round"/><rect x="20" y="27" width="2.5" height="10" rx="1" fill="#64748b"/><rect x="41.5" y="27" width="2.5" height="10" rx="1" fill="#64748b"/><rect x="15.5" y="19" width="3.5" height="26" rx="1.5" fill="#10b981"/><rect x="11.5" y="23" width="3" height="18" rx="1" fill="#3b82f6"/><rect x="45" y="19" width="3.5" height="26" rx="1.5" fill="#10b981"/><rect x="49.5" y="23" width="3" height="18" rx="1" fill="#3b82f6"/></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%2309090b%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%2327272a%22%2F%3E%3Cline%20x1%3D%228%22%20y1%3D%2232%22%20x2%3D%2256%22%20y2%3D%2232%22%20stroke%3D%22%2394a3b8%22%20stroke-width%3D%223%22%20stroke-linecap%3D%22round%22%2F%3E%3Cline%20x1%3D%2224%22%20y1%3D%2232%22%20x2%3D%2240%22%20y2%3D%2232%22%20stroke%3D%22%23cbd5e1%22%20stroke-width%3D%223.5%22%20stroke-linecap%3D%22round%22%2F%3E%3Crect%20x%3D%2220%22%20y%3D%2227%22%20width%3D%222.5%22%20height%3D%2210%22%20rx%3D%221%22%20fill%3D%22%2364748b%22%2F%3E%3Crect%20x%3D%2241.5%22%20y%3D%2227%22%20width%3D%222.5%22%20height%3D%2210%22%20rx%3D%221%22%20fill%3D%22%2364748b%22%2F%3E%3Crect%20x%3D%2215.5%22%20y%3D%2219%22%20width%3D%223.5%22%20height%3D%2226%22%20rx%3D%221.5%22%20fill%3D%22%2310b981%22%2F%3E%3Crect%20x%3D%2211.5%22%20y%3D%2223%22%20width%3D%223%22%20height%3D%2218%22%20rx%3D%221%22%20fill%3D%22%233b82f6%22%2F%3E%3Crect%20x%3D%2245%22%20y%3D%2219%22%20width%3D%223.5%22%20height%3D%2226%22%20rx%3D%221.5%22%20fill%3D%22%2310b981%22%2F%3E%3Crect%20x%3D%2249.5%22%20y%3D%2223%22%20width%3D%223%22%20height%3D%2218%22%20rx%3D%221%22%20fill%3D%22%233b82f6%22%2F%3E%3C%2Fsvg%3E'
    },
    {
      id: 'kettlebell',
      title: 'Гиря',
      category: 'equipment',
      categoryName: 'Экипировка',
      themeColor: '#064e3b',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#064e3b"/><circle cx="32" cy="32" r="31.5" stroke="#10b981" stroke-opacity="0.3"/><path d="M22 25V20C22 16.5 26 14 32 14C38 14 42 16.5 42 20V25" stroke="#94a3b8" stroke-width="4.5" stroke-linecap="round" fill="none"/><path d="M19 32C19 25 25 24 32 24C39 24 45 25 45 32C46.5 41 41 49 37 49H27C23 49 17.5 41 19 32Z" fill="#18181b"/><circle cx="32" cy="37" r="5" stroke="#10b981" stroke-width="1.5" fill="none"/><text x="32" y="39.5" text-anchor="middle" font-size="5" font-family="-apple-system, system-ui, sans-serif" font-weight="700" fill="#10b981">24</text></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%23064e3b%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%2310b981%22%20stroke-opacity%3D%220.3%22%2F%3E%3Cpath%20d%3D%22M22%2025V20C22%2016.5%2026%2014%2032%2014C38%2014%2042%2016.5%2042%2020V25%22%20stroke%3D%22%2394a3b8%22%20stroke-width%3D%224.5%22%20stroke-linecap%3D%22round%22%20fill%3D%22none%22%2F%3E%3Cpath%20d%3D%22M19%2032C19%2025%2025%2024%2032%2024C39%2024%2045%2025%2045%2032C46.5%2041%2041%2049%2037%2049H27C23%2049%2017.5%2041%2019%2032Z%22%20fill%3D%22%2318181b%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2237%22%20r%3D%225%22%20stroke%3D%22%2310b981%22%20stroke-width%3D%221.5%22%20fill%3D%22none%22%2F%3E%3Ctext%20x%3D%2232%22%20y%3D%2239.5%22%20text-anchor%3D%22middle%22%20font-size%3D%225%22%20font-family%3D%22-apple-system%2C%20system-ui%2C%20sans-serif%22%20font-weight%3D%22700%22%20fill%3D%22%2310b981%22%3E24%3C%2Ftext%3E%3C%2Fsvg%3E'
    },
    {
      id: 'dumbbell',
      title: 'Гантель',
      category: 'equipment',
      categoryName: 'Экипировка',
      themeColor: '#0f172a',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#0f172a"/><circle cx="32" cy="32" r="31.5" stroke="#334155" stroke-opacity="0.6"/><g transform="rotate(-45 32 32)"><rect x="22" y="29.5" width="20" height="5" rx="1.5" fill="#94a3b8"/><line x1="26" y1="30" x2="26" y2="34" stroke="#64748b" stroke-width="1"/><line x1="32" y1="30" x2="32" y2="34" stroke="#64748b" stroke-width="1"/><line x1="38" y1="30" x2="38" y2="34" stroke="#64748b" stroke-width="1"/><rect x="17" y="24" width="4" height="16" rx="2" fill="#10b981"/><rect x="12" y="26" width="4" height="12" rx="2" fill="#38bdf8"/><rect x="43" y="24" width="4" height="16" rx="2" fill="#10b981"/><rect x="48" y="26" width="4" height="12" rx="2" fill="#38bdf8"/></g></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%230f172a%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%23334155%22%20stroke-opacity%3D%220.6%22%2F%3E%3Cg%20transform%3D%22rotate(-45%2032%2032)%22%3E%3Crect%20x%3D%2222%22%20y%3D%2229.5%22%20width%3D%2220%22%20height%3D%225%22%20rx%3D%221.5%22%20fill%3D%22%2394a3b8%22%2F%3E%3Cline%20x1%3D%2226%22%20y1%3D%2230%22%20x2%3D%2226%22%20y2%3D%2234%22%20stroke%3D%22%2364748b%22%20stroke-width%3D%221%22%2F%3E%3Cline%20x1%3D%2232%22%20y1%3D%2230%22%20x2%3D%2232%22%20y2%3D%2234%22%20stroke%3D%22%2364748b%22%20stroke-width%3D%221%22%2F%3E%3Cline%20x1%3D%2238%22%20y1%3D%2230%22%20x2%3D%2238%22%20y2%3D%2234%22%20stroke%3D%22%2364748b%22%20stroke-width%3D%221%22%2F%3E%3Crect%20x%3D%2217%22%20y%3D%2224%22%20width%3D%224%22%20height%3D%2216%22%20rx%3D%222%22%20fill%3D%22%2310b981%22%2F%3E%3Crect%20x%3D%2212%22%20y%3D%2226%22%20width%3D%224%22%20height%3D%2212%22%20rx%3D%222%22%20fill%3D%22%2338bdf8%22%2F%3E%3Crect%20x%3D%2243%22%20y%3D%2224%22%20width%3D%224%22%20height%3D%2216%22%20rx%3D%222%22%20fill%3D%22%2310b981%22%2F%3E%3Crect%20x%3D%2248%22%20y%3D%2226%22%20width%3D%224%22%20height%3D%2212%22%20rx%3D%222%22%20fill%3D%22%2338bdf8%22%2F%3E%3C%2Fg%3E%3C%2Fsvg%3E'
    },
    {
      id: 'stopwatch',
      title: 'Секундомер',
      category: 'equipment',
      categoryName: 'Экипировка',
      themeColor: '#0c4a6e',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#0c4a6e"/><circle cx="32" cy="32" r="31.5" stroke="#38bdf8" stroke-opacity="0.4"/><rect x="29" y="11" width="6" height="4" rx="1" fill="#94a3b8"/><rect x="28" y="8" width="8" height="3" rx="1" fill="#f59e0b"/><line x1="44" y1="18" x2="47" y2="15" stroke="#94a3b8" stroke-width="2.5" stroke-linecap="round"/><circle cx="32" cy="35" r="18" fill="#0f172a" stroke="#cbd5e1" stroke-width="2.5"/><circle cx="32" cy="35" r="14" fill="#020617"/><line x1="32" y1="23" x2="32" y2="25" stroke="#64748b" stroke-width="1.5"/><line x1="32" y1="45" x2="32" y2="47" stroke="#64748b" stroke-width="1.5"/><line x1="20" y1="35" x2="22" y2="35" stroke="#64748b" stroke-width="1.5"/><line x1="42" y1="35" x2="44" y2="35" stroke="#64748b" stroke-width="1.5"/><line x1="32" y1="35" x2="38" y2="27" stroke="#f59e0b" stroke-width="2" stroke-linecap="round"/><circle cx="32" cy="35" r="2" fill="#f59e0b"/></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%230c4a6e%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%2338bdf8%22%20stroke-opacity%3D%220.4%22%2F%3E%3Crect%20x%3D%2229%22%20y%3D%2211%22%20width%3D%226%22%20height%3D%224%22%20rx%3D%221%22%20fill%3D%22%2394a3b8%22%2F%3E%3Crect%20x%3D%2228%22%20y%3D%228%22%20width%3D%228%22%20height%3D%223%22%20rx%3D%221%22%20fill%3D%22%23f59e0b%22%2F%3E%3Cline%20x1%3D%2244%22%20y1%3D%2218%22%20x2%3D%2247%22%20y2%3D%2215%22%20stroke%3D%22%2394a3b8%22%20stroke-width%3D%222.5%22%20stroke-linecap%3D%22round%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2235%22%20r%3D%2218%22%20fill%3D%22%230f172a%22%20stroke%3D%22%23cbd5e1%22%20stroke-width%3D%222.5%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2235%22%20r%3D%2214%22%20fill%3D%22%23020617%22%2F%3E%3Cline%20x1%3D%2232%22%20y1%3D%2223%22%20x2%3D%2232%22%20y2%3D%2225%22%20stroke%3D%22%2364748b%22%20stroke-width%3D%221.5%22%2F%3E%3Cline%20x1%3D%2232%22%20y1%3D%2245%22%20x2%3D%2232%22%20y2%3D%2247%22%20stroke%3D%22%2364748b%22%20stroke-width%3D%221.5%22%2F%3E%3Cline%20x1%3D%2220%22%20y1%3D%2235%22%20x2%3D%2222%22%20y2%3D%2235%22%20stroke%3D%22%2364748b%22%20stroke-width%3D%221.5%22%2F%3E%3Cline%20x1%3D%2242%22%20y1%3D%2235%22%20x2%3D%2244%22%20y2%3D%2235%22%20stroke%3D%22%2364748b%22%20stroke-width%3D%221.5%22%2F%3E%3Cline%20x1%3D%2232%22%20y1%3D%2235%22%20x2%3D%2238%22%20y2%3D%2227%22%20stroke%3D%22%23f59e0b%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2235%22%20r%3D%222%22%20fill%3D%22%23f59e0b%22%2F%3E%3C%2Fsvg%3E'
    },
    {
      id: 'boxing',
      title: 'Бокс',
      category: 'equipment',
      categoryName: 'Экипировка',
      themeColor: '#4c0519',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#4c0519"/><circle cx="32" cy="32" r="31.5" stroke="#f43f5e" stroke-opacity="0.3"/><rect x="23" y="44" width="18" height="7" rx="2" fill="#1e293b"/><rect x="25" y="46" width="14" height="3" rx="1" fill="#f43f5e"/><path d="M21 42C19 36 17 28 23 20C28 14 39 14 43 20C47 26 44 38 41 42H21Z" fill="#e11d48"/><path d="M20 28C17 28 15 32 17 36C18 38 21 39 23 37" fill="#be123c"/><path d="M23 24C27 24 39 24 41 28" stroke="#fda4af" stroke-width="1.5" stroke-linecap="round"/></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%234c0519%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%23f43f5e%22%20stroke-opacity%3D%220.3%22%2F%3E%3Crect%20x%3D%2223%22%20y%3D%2244%22%20width%3D%2218%22%20height%3D%227%22%20rx%3D%222%22%20fill%3D%22%231e293b%22%2F%3E%3Crect%20x%3D%2225%22%20y%3D%2246%22%20width%3D%2214%22%20height%3D%223%22%20rx%3D%221%22%20fill%3D%22%23f43f5e%22%2F%3E%3Cpath%20d%3D%22M21%2042C19%2036%2017%2028%2023%2020C28%2014%2039%2014%2043%2020C47%2026%2044%2038%2041%2042H21Z%22%20fill%3D%22%23e11d48%22%2F%3E%3Cpath%20d%3D%22M20%2028C17%2028%2015%2032%2017%2036C18%2038%2021%2039%2023%2037%22%20fill%3D%22%23be123c%22%2F%3E%3Cpath%20d%3D%22M23%2024C27%2024%2039%2024%2041%2028%22%20stroke%3D%22%23fda4af%22%20stroke-width%3D%221.5%22%20stroke-linecap%3D%22round%22%2F%3E%3C%2Fsvg%3E'
    },
    {
      id: 'medal',
      title: 'Медаль',
      category: 'equipment',
      categoryName: 'Экипировка',
      themeColor: '#451a03',
      svg: `<svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="32" fill="#451a03"/><circle cx="32" cy="32" r="31.5" stroke="#f59e0b" stroke-opacity="0.3"/><path d="M22 10L32 29L26 29L17 10H22Z" fill="#10b981"/><path d="M42 10L32 29L38 29L47 10H42Z" fill="#0284c7"/><circle cx="32" cy="30" r="3" fill="#d97706"/><circle cx="32" cy="40" r="14" fill="#f59e0b"/><circle cx="32" cy="40" r="11.5" stroke="#b45309" stroke-width="1.5" fill="#fbbf24"/><path d="M32 34.5L33.7 38L37.5 38.5L34.7 41.2L35.4 45L32 43.1L28.6 45L29.3 41.2L26.5 38.5L30.3 38L32 34.5Z" fill="#b45309"/></svg>`,
      dataUri: 'data:image/svg+xml;utf8,%3Csvg%20viewBox%3D%220%200%2064%2064%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%23451a03%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2231.5%22%20stroke%3D%22%23f59e0b%22%20stroke-opacity%3D%220.3%22%2F%3E%3Cpath%20d%3D%22M22%2010L32%2029L26%2029L17%2010H22Z%22%20fill%3D%22%2310b981%22%2F%3E%3Cpath%20d%3D%22M42%2010L32%2029L38%2029L47%2010H42Z%22%20fill%3D%22%230284c7%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2230%22%20r%3D%223%22%20fill%3D%22%23d97706%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2240%22%20r%3D%2214%22%20fill%3D%22%23f59e0b%22%2F%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2240%22%20r%3D%2211.5%22%20stroke%3D%22%23b45309%22%20stroke-width%3D%221.5%22%20fill%3D%22%23fbbf24%22%2F%3E%3Cpath%20d%3D%22M32%2034.5L33.7%2038L37.5%2038.5L34.7%2041.2L35.4%2045L32%2043.1L28.6%2045L29.3%2041.2L26.5%2038.5L30.3%2038L32%2034.5Z%22%20fill%3D%22%23b45309%22%2F%3E%3C%2Fsvg%3E'
    }
  ];

  function getAvatarById(id) {
    if (!id) return AVATAR_CATALOG[0];
    const cleanId = id.replace('default:', '');
    return AVATAR_CATALOG.find(a => a.id === cleanId) || AVATAR_CATALOG[0];
  }

  function getAvatarByDataUri(dataUri) {
    if (!dataUri) return null;
    return AVATAR_CATALOG.find(a => a.dataUri === dataUri) || null;
  }

  function isValidAvatarPayload(payload) {
    if (!payload || typeof payload !== 'string') return false;
    const cleanId = payload.replace('default:', '');
    if (AVATAR_CATALOG.some(a => a.id === cleanId)) return true;
    if (AVATAR_CATALOG.some(a => a.dataUri === payload)) return true;
    if (payload.startsWith('data:image/svg+xml') && (payload.includes('%3Csvg') || payload.includes('<svg'))) {
      return true;
    }
    return false;
  }

  const AvatarModule = {
    AVATAR_CATALOG,
    getAvatarById,
    getAvatarByDataUri,
    isValidAvatarPayload
  };

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = AvatarModule;
  }
  if (typeof window !== 'undefined') {
    window.AvatarModule = AvatarModule;
  }
})(typeof globalThis !== 'undefined' ? globalThis : this);
