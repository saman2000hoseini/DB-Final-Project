select user_register('Saman2000h', 'pashmak pashmak', 'saman', 'hoseini', 'tehran sadeghieh', 'student');
select user_register('admindb', '12345678', 'saman', 'hoseini', 'tehran sadeghieh', 'admin');

select user_login('saman2000h', 'pashmak pashmak');

select user_login('admindb', '12345678');

select user_get_info(
               '\x4deba1ad419e74b1af0bacbb602594c2f273b37a7b43decd1b32de688cfa20350f98cc23ac88d5832c102ab5fd6e337689398b1ee69147fe73410b86989ee78b');

select publisher_insert(
               '\x133ec2cb2e23d42e5a8a7d20fef962ed23b62a0605e5e1244bcbaa01f1f79e4aa1638aa1b88de51717614385ba9468a4e0e118298aa1eff187ccc50de2a10636',
               'MIT Press', 'massachusetts', 'https://mitpress.mit.edu'
           );

select book_insert(
               '\x133ec2cb2e23d42e5a8a7d20fef962ed23b62a0605e5e1244bcbaa01f1f79e4aa1638aa1b88de51717614385ba9468a4e0e118298aa1eff187ccc50de2a10636',
               'introduction to algorithms', 'computer science', 50000, 2300, 'educational', current_date, 1
           );

select user_search(
               '\xfb096cba7706d5ba3bddf405ea01c9880308edc86572d0ca463ea50903de584a293e73bbb22af0426ce7e5d3ad87deb7a6982fec4980272296435f87087bb015'
           , 'saman2000h');