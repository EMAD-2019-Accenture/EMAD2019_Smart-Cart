package it.unisa.scanapp.web.rest;

import it.unisa.scanapp.SmartCartApp;
import it.unisa.scanapp.domain.Allergen;
import it.unisa.scanapp.repository.AllergenRepository;
import it.unisa.scanapp.web.rest.errors.ExceptionTranslator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;

import static it.unisa.scanapp.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link AllergenResource} REST controller.
 */
@SpringBootTest(classes = SmartCartApp.class)
public class AllergenResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    @Autowired
    private AllergenRepository allergenRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restAllergenMockMvc;

    private Allergen allergen;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final AllergenResource allergenResource = new AllergenResource(allergenRepository);
        this.restAllergenMockMvc = MockMvcBuilders.standaloneSetup(allergenResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Allergen createEntity(EntityManager em) {
        Allergen allergen = new Allergen()
            .name(DEFAULT_NAME);
        return allergen;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Allergen createUpdatedEntity(EntityManager em) {
        Allergen allergen = new Allergen()
            .name(UPDATED_NAME);
        return allergen;
    }

    @BeforeEach
    public void initTest() {
        allergen = createEntity(em);
    }

    @Test
    @Transactional
    public void createAllergen() throws Exception {
        int databaseSizeBeforeCreate = allergenRepository.findAll().size();

        // Create the Allergen
        restAllergenMockMvc.perform(post("/api/allergens")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(allergen)))
            .andExpect(status().isCreated());

        // Validate the Allergen in the database
        List<Allergen> allergenList = allergenRepository.findAll();
        assertThat(allergenList).hasSize(databaseSizeBeforeCreate + 1);
        Allergen testAllergen = allergenList.get(allergenList.size() - 1);
        assertThat(testAllergen.getName()).isEqualTo(DEFAULT_NAME);
    }

    @Test
    @Transactional
    public void createAllergenWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = allergenRepository.findAll().size();

        // Create the Allergen with an existing ID
        allergen.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restAllergenMockMvc.perform(post("/api/allergens")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(allergen)))
            .andExpect(status().isBadRequest());

        // Validate the Allergen in the database
        List<Allergen> allergenList = allergenRepository.findAll();
        assertThat(allergenList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = allergenRepository.findAll().size();
        // set the field null
        allergen.setName(null);

        // Create the Allergen, which fails.

        restAllergenMockMvc.perform(post("/api/allergens")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(allergen)))
            .andExpect(status().isBadRequest());

        List<Allergen> allergenList = allergenRepository.findAll();
        assertThat(allergenList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllAllergens() throws Exception {
        // Initialize the database
        allergenRepository.saveAndFlush(allergen);

        // Get all the allergenList
        restAllergenMockMvc.perform(get("/api/allergens?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(allergen.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())));
    }
    
    @Test
    @Transactional
    public void getAllergen() throws Exception {
        // Initialize the database
        allergenRepository.saveAndFlush(allergen);

        // Get the allergen
        restAllergenMockMvc.perform(get("/api/allergens/{id}", allergen.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(allergen.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingAllergen() throws Exception {
        // Get the allergen
        restAllergenMockMvc.perform(get("/api/allergens/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateAllergen() throws Exception {
        // Initialize the database
        allergenRepository.saveAndFlush(allergen);

        int databaseSizeBeforeUpdate = allergenRepository.findAll().size();

        // Update the allergen
        Allergen updatedAllergen = allergenRepository.findById(allergen.getId()).get();
        // Disconnect from session so that the updates on updatedAllergen are not directly saved in db
        em.detach(updatedAllergen);
        updatedAllergen
            .name(UPDATED_NAME);

        restAllergenMockMvc.perform(put("/api/allergens")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedAllergen)))
            .andExpect(status().isOk());

        // Validate the Allergen in the database
        List<Allergen> allergenList = allergenRepository.findAll();
        assertThat(allergenList).hasSize(databaseSizeBeforeUpdate);
        Allergen testAllergen = allergenList.get(allergenList.size() - 1);
        assertThat(testAllergen.getName()).isEqualTo(UPDATED_NAME);
    }

    @Test
    @Transactional
    public void updateNonExistingAllergen() throws Exception {
        int databaseSizeBeforeUpdate = allergenRepository.findAll().size();

        // Create the Allergen

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAllergenMockMvc.perform(put("/api/allergens")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(allergen)))
            .andExpect(status().isBadRequest());

        // Validate the Allergen in the database
        List<Allergen> allergenList = allergenRepository.findAll();
        assertThat(allergenList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteAllergen() throws Exception {
        // Initialize the database
        allergenRepository.saveAndFlush(allergen);

        int databaseSizeBeforeDelete = allergenRepository.findAll().size();

        // Delete the allergen
        restAllergenMockMvc.perform(delete("/api/allergens/{id}", allergen.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Allergen> allergenList = allergenRepository.findAll();
        assertThat(allergenList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Allergen.class);
        Allergen allergen1 = new Allergen();
        allergen1.setId(1L);
        Allergen allergen2 = new Allergen();
        allergen2.setId(allergen1.getId());
        assertThat(allergen1).isEqualTo(allergen2);
        allergen2.setId(2L);
        assertThat(allergen1).isNotEqualTo(allergen2);
        allergen1.setId(null);
        assertThat(allergen1).isNotEqualTo(allergen2);
    }
}
